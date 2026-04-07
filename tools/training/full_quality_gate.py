#!/usr/bin/env python3
"""Run synthetic + golden checks and fail on regressions."""

from __future__ import annotations

import argparse
import json
import subprocess
from pathlib import Path

import numpy as np
from transformers import AutoModelForSequenceClassification
from transformers import AutoTokenizer


def read_golden(path: Path):
    rows = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            rows.append(json.loads(line))
    return rows


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="tools/training/artifacts/hf_model")
    parser.add_argument("--golden", default="tools/training/data/golden.manual.jsonl")
    parser.add_argument("--min-golden-acc", type=float, default=0.95)
    parser.add_argument("--max-length", type=int, default=64)
    args = parser.parse_args()

    # Kotlin synthetic gate
    subprocess.run(
        [
            "./gradlew",
            ":app:testDebugUnitTest",
            "--tests",
            "com.chelovecheck.data.analytics.pipeline.LexicalAliasMatcherSyntheticDatasetTest.allDatasets_aggregateSummary_1m",
        ],
        check=True,
    )

    tokenizer = AutoTokenizer.from_pretrained(args.model)
    model = AutoModelForSequenceClassification.from_pretrained(args.model)
    model.eval()
    golden = read_golden(Path(args.golden))
    texts = [" ".join(row["text"].lower().split()) for row in golden]
    labels = [row["label"] for row in golden]
    enc = tokenizer(
        texts,
        truncation=True,
        padding=True,
        max_length=args.max_length,
        return_tensors="pt",
    )
    logits = model(**enc).logits.detach().cpu().numpy()
    pred_ids = np.argmax(logits, axis=-1)
    preds = [model.config.id2label[int(idx)] for idx in pred_ids]
    correct = sum(1 for y, p in zip(labels, preds) if y == p)
    acc = correct / len(labels)
    print(f"Golden accuracy={acc:.4f} ({correct}/{len(labels)})")
    if acc < args.min_golden_acc:
        raise SystemExit(f"Golden accuracy below threshold: {acc:.4f} < {args.min_golden_acc:.4f}")


if __name__ == "__main__":
    main()
