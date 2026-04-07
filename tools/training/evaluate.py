#!/usr/bin/env python3
"""Evaluate transformer classifier and print confusion summary."""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import List, Tuple

import numpy as np
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from transformers import AutoModelForSequenceClassification
from transformers import AutoTokenizer


def read_jsonl(path: Path) -> Tuple[List[str], List[str]]:
    texts: List[str] = []
    labels: List[str] = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            row = json.loads(line)
            texts.append(row["normalized"])
            labels.append(row["label"])
    return texts, labels


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir", default="tools/training/data")
    parser.add_argument("--model", default="tools/training/artifacts/hf_model")
    parser.add_argument("--batch-size", type=int, default=128)
    parser.add_argument("--max-length", type=int, default=64)
    args = parser.parse_args()

    tokenizer = AutoTokenizer.from_pretrained(args.model)
    model = AutoModelForSequenceClassification.from_pretrained(args.model)
    model.eval()
    x_test, y_test = read_jsonl(Path(args.data_dir) / "test.jsonl")
    y_pred: List[str] = []
    with np.errstate(all="ignore"):
        for i in range(0, len(x_test), args.batch_size):
            batch = x_test[i:i + args.batch_size]
            enc = tokenizer(
                batch,
                truncation=True,
                padding=True,
                max_length=args.max_length,
                return_tensors="pt",
            )
            logits = model(**enc).logits.detach().cpu().numpy()
            pred_ids = np.argmax(logits, axis=-1)
            for pid in pred_ids:
                y_pred.append(model.config.id2label[int(pid)])

    acc = accuracy_score(y_test, y_pred)
    print(f"Test accuracy={acc:.4f} rows={len(x_test)}")
    print(classification_report(y_test, y_pred, digits=4))

    labels = sorted(set(y_test))
    matrix = confusion_matrix(y_test, y_pred, labels=labels)
    top_confusions = Counter()
    for i, expected in enumerate(labels):
        for j, predicted in enumerate(labels):
            if i == j:
                continue
            n = int(matrix[i][j])
            if n > 0:
                top_confusions[f"{expected}->{predicted}"] += n
    print("Top confusions:", top_confusions.most_common(12))


if __name__ == "__main__":
    main()
