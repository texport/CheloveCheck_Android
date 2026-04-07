#!/usr/bin/env python3
"""Copy trained ONNX/tokenizer artifacts into Android assets layout."""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--onnx", default="tools/training/artifacts/model_fp16.onnx")
    parser.add_argument("--tokenizer-dir", default="tools/training/artifacts/hf_model")
    parser.add_argument("--assets-root", default="app/src/main/assets/ml/distiluse-base-multilingual-cased-v2")
    args = parser.parse_args()

    assets_root = Path(args.assets_root)
    onnx_dir = assets_root / "onnx"
    onnx_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(args.onnx, onnx_dir / "model_fp16.onnx")

    for name in ("tokenizer.json", "tokenizer_config.json", "vocab.txt", "special_tokens_map.json"):
        src = Path(args.tokenizer_dir) / name
        if src.exists():
            shutil.copy2(src, assets_root / name)

    print(f"Integrated ONNX + tokenizer into {assets_root}")


if __name__ == "__main__":
    main()
