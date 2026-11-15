#!/usr/bin/env python3
"""Quality gate para herramientas que solo reportan en SARIF (ADR-005).

CodeQL publica sus hallazgos en la pestana Security pero no hace fallar el
job. Este script lee los SARIF, cuenta los hallazgos cuya severidad de
seguridad (propiedad "security-severity", escala CVSS 0-10) es mayor o igual
al umbral y termina con codigo 1 si queda alguno sin tratar.

Un hallazgo "tratado" es uno listado en el allowlist (falso positivo o riesgo
aceptado, con su justificacion en docs/seguridad.md).

Uso: sarif_gate.py <archivo-o-carpeta> --min-severity 7.0 [--allowlist archivo]
"""
import argparse
import json
import pathlib
import sys


def load_allowlist(path):
    allowed = set()
    if not path:
        return allowed
    p = pathlib.Path(path)
    if not p.exists():
        return allowed
    for line in p.read_text(encoding="utf-8").splitlines():
        line = line.split("#", 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        allowed.add((parts[0], parts[1] if len(parts) > 1 else "*"))
    return allowed


def rule_index(run):
    rules = {}
    tool = run.get("tool", {})
    for component in [tool.get("driver", {})] + tool.get("extensions", []):
        for rule in component.get("rules", []) or []:
            rules[rule.get("id")] = rule
    return rules


def severity_of(result, rules):
    rule = rules.get(result.get("ruleId"), {})
    props = dict(rule.get("properties", {}) or {})
    props.update(result.get("properties", {}) or {})
    raw = props.get("security-severity")
    if raw is not None:
        try:
            return float(raw)
        except ValueError:
            pass
    # Sin puntaje: un "error" cuenta como alto.
    level = result.get("level") or rule.get("defaultConfiguration", {}).get("level")
    return 7.0 if level == "error" else 0.0


def location_of(result):
    for loc in result.get("locations", []) or []:
        uri = loc.get("physicalLocation", {}).get("artifactLocation", {}).get("uri")
        line = loc.get("physicalLocation", {}).get("region", {}).get("startLine", "?")
        if uri:
            return uri, line
    return "?", "?"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("path")
    ap.add_argument("--min-severity", type=float, default=7.0)
    ap.add_argument("--allowlist")
    args = ap.parse_args()

    root = pathlib.Path(args.path)
    files = sorted(root.rglob("*.sarif")) if root.is_dir() else [root]
    if not files:
        print(f"No se encontro ningun SARIF en {root}", file=sys.stderr)
        return 1

    allowed = load_allowlist(args.allowlist)
    blocking, tolerated, total = [], [], 0
    for f in files:
        data = json.loads(f.read_text(encoding="utf-8"))
        for run in data.get("runs", []):
            rules = rule_index(run)
            for result in run.get("results", []) or []:
                total += 1
                sev = severity_of(result, rules)
                if sev < args.min_severity:
                    continue
                uri, line = location_of(result)
                rid = result.get("ruleId", "?")
                entry = f"{sev:4.1f}  {rid}  {uri}:{line}"
                if (rid, uri) in allowed or (rid, "*") in allowed:
                    tolerated.append(entry)
                else:
                    blocking.append(entry)

    print(f"Hallazgos totales: {total}. Umbral: severidad >= {args.min_severity}.")
    for e in tolerated:
        print(f"  tratado (allowlist): {e}")
    for e in blocking:
        print(f"  BLOQUEANTE: {e}")
    if blocking:
        print(f"{len(blocking)} hallazgo(s) por encima del umbral sin tratar. Ver docs/seguridad.md.")
        return 1
    print("Sin hallazgos por encima del umbral.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
