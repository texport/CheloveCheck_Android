# Training Pipeline (Hybrid)

This folder contains a reproducible training loop used in the hybrid retraining plan:

1. prepare deterministic datasets (`10 x 100k` by default),
2. fine-tune a transformer classifier (HF/PyTorch),
3. evaluate quality and confusion matrix,
4. export Android-compatible encoder ONNX + runtime smoke check.

## Quick start

```bash
python3 -m pip install -r tools/training/requirements.txt
python3 tools/training/prepare_dataset.py --datasets 10 --rows-per-dataset 100000 --seed 42
python3 tools/training/train.py --epochs 1 --batch-size 32
python3 tools/training/evaluate.py
python3 tools/training/export_onnx.py
python3 tools/training/full_quality_gate.py
```

## Determinism

- Explicit `--seed` for dataset generation.
- Fixed split ratios `80/10/10`.
- Model training uses fixed seed (`--seed`).

## Artifacts

- `tools/training/data/*.jsonl` - prepared datasets and splits.
- `tools/training/artifacts/hf_model/` - fine-tuned transformer model + tokenizer.
- `tools/training/artifacts/model_fp16.onnx` - exported encoder ONNX candidate.

## Notes for Android integration

Current Android embedding runtime expects transformer-style tensor inputs (`input_ids`, `attention_mask`, optional `token_type_ids`).
`export_onnx.py` now exports exactly this contract and validates it with onnxruntime smoke inference.

## Current baseline/candidate snapshot

- Synthetic split (`10x100k`): accuracy `1.0000`, no top confusions.
- Golden manual set: expected to stay `>= 0.95` in `full_quality_gate.py`.
