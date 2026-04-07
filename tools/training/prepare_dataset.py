#!/usr/bin/env python3
"""Prepare deterministic synthetic train/val/test splits for category training."""

from __future__ import annotations

import argparse
import json
import random
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List


@dataclass(frozen=True)
class Lexicon:
    category_id: str
    primary: List[str]
    equivalents: List[List[str]]


LEXICONS = [
    Lexicon(
        category_id="01.1",
        primary=["milk", "bread", "apple"],
        equivalents=[["milk", "молоко", "сүт"], ["bread", "хлеб", "нан"], ["apple", "яблоко", "алма"]],
    ),
    Lexicon(
        category_id="07.3",
        primary=["bus", "taxi", "metro"],
        equivalents=[["bus", "автобус", "жол жүру"], ["taxi", "такси", "taxi"], ["metro", "метро", "metro"]],
    ),
    Lexicon(
        category_id="03.1",
        primary=["shirt", "jeans", "dress"],
        equivalents=[["shirt", "рубашка", "көйлек"], ["jeans", "джинсы", "шалбар"], ["dress", "платье", "көйлек"]],
    ),
]

BRANDS = ["magnum", "small", "dostar", "global", "arzan", "freshmart"]
QTY = ["1л", "500г", "2шт", "250 мл", "0.75l", "xl"]
NOISE = ["promo", "скидка", "акция", "fresh", "new", "2026"]


def normalize(text: str) -> str:
    return " ".join(text.lower().split())


def build_rows(dataset_id: int, total_rows: int, seed: int) -> List[Dict[str, str]]:
    rnd = random.Random(seed)
    unique_count = int(total_rows * 0.55)
    repeat_count = int(total_rows * 0.25)
    semantic_count = total_rows - unique_count - repeat_count
    rows: List[Dict[str, str]] = []

    for i in range(unique_count):
        lex = LEXICONS[(i + dataset_id) % len(LEXICONS)]
        t1 = lex.primary[i % len(lex.primary)]
        t2 = lex.primary[(i + 1) % len(lex.primary)]
        text = f"{t1} {t2} {BRANDS[(i + dataset_id) % len(BRANDS)]} {QTY[i % len(QTY)]} {NOISE[(i * 3) % len(NOISE)]} d{dataset_id}_u{i}"
        rows.append(
            {
                "dataset_id": f"{dataset_id:02d}",
                "kind": "unique",
                "text": text,
                "label": lex.category_id,
                "group_key": f"u-{dataset_id}-{i}",
                "normalized": normalize(text),
            }
        )

    repeat_families = max(1, repeat_count // 5)
    per_family = max(1, repeat_count // repeat_families)
    for fam in range(repeat_families):
        lex = LEXICONS[(fam + dataset_id) % len(LEXICONS)]
        base = f"{lex.primary[fam % len(lex.primary)]} {lex.primary[(fam + 2) % len(lex.primary)]} {BRANDS[fam % len(BRANDS)]}"
        for _ in range(per_family):
            rows.append(
                {
                    "dataset_id": f"{dataset_id:02d}",
                    "kind": "repeat",
                    "text": base,
                    "label": lex.category_id,
                    "group_key": f"r-{dataset_id}-{fam}",
                    "normalized": normalize(base),
                }
            )

    semantic_families = max(1, semantic_count // 5)
    semantic_per_family = max(1, semantic_count // semantic_families)
    for fam in range(semantic_families):
        lex = LEXICONS[(fam + 2 * dataset_id) % len(LEXICONS)]
        eq1 = lex.equivalents[fam % len(lex.equivalents)]
        eq2 = lex.equivalents[(fam + 1) % len(lex.equivalents)]
        for s in range(semantic_per_family):
            v1 = eq1[(s + fam) % len(eq1)]
            v2 = eq2[(s + 1) % len(eq2)]
            text = f"{v1} {v2} {BRANDS[(fam + s) % len(BRANDS)]} {QTY[(fam + s) % len(QTY)]} {NOISE[(fam + s) % len(NOISE)]}"
            rows.append(
                {
                    "dataset_id": f"{dataset_id:02d}",
                    "kind": "semantic",
                    "text": text,
                    "label": lex.category_id,
                    "group_key": f"s-{dataset_id}-{fam}",
                    "normalized": normalize(text),
                }
            )

    rows = rows[:total_rows]
    rnd.shuffle(rows)
    return rows


def write_jsonl(path: Path, rows: List[Dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8") as f:
        for row in rows:
            f.write(json.dumps(row, ensure_ascii=False) + "\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", default="tools/training/data")
    parser.add_argument("--datasets", type=int, default=10)
    parser.add_argument("--rows-per-dataset", type=int, default=100_000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--golden-path", default="tools/training/data/golden.manual.jsonl")
    parser.add_argument("--golden-to-test-only", action="store_true", default=True)
    args = parser.parse_args()

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    all_rows: List[Dict[str, str]] = []
    for dataset_id in range(1, args.datasets + 1):
        rows = build_rows(dataset_id, args.rows_per_dataset, args.seed + dataset_id * 1000)
        write_jsonl(output_dir / f"dataset_{dataset_id:02d}.jsonl", rows)
        all_rows.extend(rows)

    # deterministic split
    rnd = random.Random(args.seed)
    rnd.shuffle(all_rows)
    n = len(all_rows)
    train = all_rows[: int(n * 0.8)]
    val = all_rows[int(n * 0.8): int(n * 0.9)]
    test = all_rows[int(n * 0.9):]
    golden_path = Path(args.golden_path)
    if golden_path.exists():
        with golden_path.open("r", encoding="utf-8") as f:
            for line in f:
                row = json.loads(line)
                text = normalize(row["text"])
                golden_row = {
                    "dataset_id": "golden",
                    "kind": "golden",
                    "text": row["text"],
                    "label": row["label"],
                    "group_key": f"golden-{text}",
                    "normalized": text,
                }
                if args.golden_to_test_only:
                    test.append(golden_row)
                else:
                    train.append(golden_row)
        rnd.shuffle(train)
        rnd.shuffle(test)

    write_jsonl(output_dir / "train.jsonl", train)
    write_jsonl(output_dir / "val.jsonl", val)
    write_jsonl(output_dir / "test.jsonl", test)
    print(f"Prepared rows: total={n} train={len(train)} val={len(val)} test={len(test)}")


if __name__ == "__main__":
    main()
