#!/usr/bin/env python3
"""Train a transformer category classifier on prepared synthetic/golden data."""

from __future__ import annotations

import argparse
import json
import random
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
import torch
from transformers import AutoModelForSequenceClassification
from transformers import AutoTokenizer
from transformers import Trainer
from transformers import TrainingArguments


def read_jsonl(path: Path) -> Tuple[List[str], List[str]]:
    texts: List[str] = []
    labels: List[str] = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            row = json.loads(line)
            texts.append(row["normalized"])
            labels.append(row["label"])
    return texts, labels


class JsonlDataset(torch.utils.data.Dataset):
    def __init__(self, texts: List[str], labels: List[int], tokenizer, max_length: int):
        self.texts = texts
        self.labels = labels
        self.tokenizer = tokenizer
        self.max_length = max_length

    def __len__(self) -> int:
        return len(self.texts)

    def __getitem__(self, idx: int) -> Dict[str, torch.Tensor]:
        enc = self.tokenizer(
            self.texts[idx],
            truncation=True,
            padding="max_length",
            max_length=self.max_length,
            return_tensors="pt",
        )
        item = {k: v.squeeze(0) for k, v in enc.items()}
        item["labels"] = torch.tensor(self.labels[idx], dtype=torch.long)
        return item


def set_seed(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir", default="tools/training/data")
    parser.add_argument("--out-dir", default="tools/training/artifacts")
    parser.add_argument("--max-rows", type=int, default=0, help="Optional cap for quick local runs")
    parser.add_argument("--model-id", default="sentence-transformers/distiluse-base-multilingual-cased-v2")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--max-length", type=int, default=64)
    parser.add_argument("--epochs", type=float, default=1.0)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--skip-eval", action="store_true")
    args = parser.parse_args()

    data_dir = Path(args.data_dir)
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    set_seed(args.seed)
    x_train, y_train_raw = read_jsonl(data_dir / "train.jsonl")
    x_val, y_val_raw = read_jsonl(data_dir / "val.jsonl")
    if args.max_rows > 0:
        x_train = x_train[: args.max_rows]
        y_train_raw = y_train_raw[: args.max_rows]
        x_val = x_val[: args.max_rows]
        y_val_raw = y_val_raw[: args.max_rows]

    label_list = sorted(set(y_train_raw + y_val_raw))
    label2id = {label: i for i, label in enumerate(label_list)}
    id2label = {i: label for label, i in label2id.items()}
    y_train = [label2id[y] for y in y_train_raw]
    y_val = [label2id[y] for y in y_val_raw]

    tokenizer = AutoTokenizer.from_pretrained(args.model_id)
    model = AutoModelForSequenceClassification.from_pretrained(
        args.model_id,
        num_labels=len(label_list),
        id2label=id2label,
        label2id=label2id,
    )
    train_ds = JsonlDataset(x_train, y_train, tokenizer, args.max_length)
    val_ds = JsonlDataset(x_val, y_val, tokenizer, args.max_length)

    training_args = TrainingArguments(
        output_dir=str(out_dir / "checkpoints"),
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        learning_rate=2e-5,
        num_train_epochs=args.epochs,
        eval_strategy="no" if args.skip_eval else "epoch",
        save_strategy="no" if args.skip_eval else "epoch",
        logging_steps=100,
        report_to=[],
        seed=args.seed,
    )

    def compute_metrics(eval_pred):
        logits, labels = eval_pred
        preds = np.argmax(logits, axis=-1)
        acc = (preds == labels).mean().item()
        return {"accuracy": acc}

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=train_ds,
        eval_dataset=val_ds,
        compute_metrics=compute_metrics,
    )
    trainer.train()
    if args.skip_eval:
        print(f"Training done without eval rows={len(x_train)}")
    else:
        metrics = trainer.evaluate()
        print(f"Validation accuracy={metrics.get('eval_accuracy', 0.0):.4f} rows={len(x_train)}")

    model_dir = out_dir / "hf_model"
    model.save_pretrained(model_dir)
    tokenizer.save_pretrained(model_dir)
    with (out_dir / "label_map.json").open("w", encoding="utf-8") as f:
        json.dump({"label2id": label2id, "id2label": {str(k): v for k, v in id2label.items()}}, f, ensure_ascii=False, indent=2)
    print(f"Saved HF model: {model_dir}")


if __name__ == "__main__":
    main()
