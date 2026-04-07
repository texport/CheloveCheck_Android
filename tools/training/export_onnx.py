#!/usr/bin/env python3
"""Export transformer encoder to ONNX and verify Android-compatible input contract."""

from __future__ import annotations

import argparse
from pathlib import Path

import torch
import onnxruntime as ort
from transformers import AutoModel
from transformers import AutoTokenizer


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="tools/training/artifacts/hf_model")
    parser.add_argument("--out", default="tools/training/artifacts/model_fp16.onnx")
    parser.add_argument("--max-length", type=int, default=64)
    args = parser.parse_args()

    tokenizer = AutoTokenizer.from_pretrained(args.model)
    model = AutoModel.from_pretrained(args.model)
    model.eval()

    sample = tokenizer(
        ["milk bread", "такси билет"],
        max_length=args.max_length,
        truncation=True,
        padding="max_length",
        return_tensors="pt",
    )
    input_names = ["input_ids", "attention_mask"]
    input_tensors = (sample["input_ids"], sample["attention_mask"])
    dynamic_axes = {
        "input_ids": {0: "batch", 1: "seq"},
        "attention_mask": {0: "batch", 1: "seq"},
        "last_hidden_state": {0: "batch", 1: "seq"},
    }
    if "token_type_ids" in sample:
        input_names.append("token_type_ids")
        input_tensors = (sample["input_ids"], sample["attention_mask"], sample["token_type_ids"])
        dynamic_axes["token_type_ids"] = {0: "batch", 1: "seq"}

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with torch.no_grad():
        torch.onnx.export(
            model,
            input_tensors,
            str(out),
            input_names=input_names,
            output_names=["last_hidden_state"],
            dynamic_axes=dynamic_axes,
            opset_version=17,
            do_constant_folding=True,
        )
    print(f"Exported ONNX: {out}")

    sess = ort.InferenceSession(str(out), providers=["CPUExecutionProvider"])
    input_defs = [(i.name, i.type, i.shape) for i in sess.get_inputs()]
    print(f"ONNX inputs={input_defs}")
    print(f"ONNX outputs={[(o.name, o.type) for o in sess.get_outputs()]}")

    # Smoke inference with Android-style named inputs.
    ort_inputs = {k: v.cpu().numpy() for k, v in sample.items() if k in {i[0] for i in input_defs}}
    output = sess.run(None, ort_inputs)
    print(f"Smoke output tensor shape={output[0].shape}")


if __name__ == "__main__":
    main()
