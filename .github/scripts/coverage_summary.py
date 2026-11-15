#!/usr/bin/env python3
"""Resume el CSV de JaCoCo por paquete para el resumen del job (Markdown)."""
import csv
import sys
from collections import defaultdict

DOMINIO = ("com.proyecto.rpg.domain", "com.proyecto.rpg.application")


def main(path):
    lines = defaultdict(lambda: [0, 0])
    try:
        with open(path, newline="", encoding="utf-8") as f:
            for row in csv.DictReader(f):
                pkg = row["PACKAGE"]
                lines[pkg][0] += int(row["LINE_COVERED"])
                lines[pkg][1] += int(row["LINE_COVERED"]) + int(row["LINE_MISSED"])
    except FileNotFoundError:
        print("No hay reporte de cobertura (las pruebas no llegaron a correr).")
        return
    print("### Cobertura de lineas por paquete\n")
    print("| Paquete | Cubiertas | Total | % |")
    print("|---|---|---|---|")
    dom_cov = dom_tot = 0
    for pkg in sorted(lines):
        cov, tot = lines[pkg]
        pct = 100.0 * cov / tot if tot else 0.0
        mark = " (dominio)" if pkg.startswith(DOMINIO) else ""
        print(f"| `{pkg}`{mark} | {cov} | {tot} | {pct:.1f} |")
        if pkg.startswith(DOMINIO):
            dom_cov += cov
            dom_tot += tot
    if dom_tot:
        print(f"\n**Dominio (domain + application): {100.0 * dom_cov / dom_tot:.1f} %** — umbral en pom.xml.")


if __name__ == "__main__":
    main(sys.argv[1])
