# ADR-005: Estrategia de seguridad del pipeline: herramientas y umbrales

Estado: aceptado e implementado.

Fecha: noviembre de 2026.

## Contexto

En el Corte 2 nada impedía que un cambio introdujera una dependencia con una vulnerabilidad conocida, una llave en un archivo de configuración o una imagen con paquetes del sistema sin parchear. El Corte 3 exige cuatro controles que detengan el pipeline al superar un umbral: SAST, SCA, detección de secretos y escaneo de la imagen. Cada grupo puede endurecer los umbrales, no relajarlos.

Hay dos restricciones prácticas. El pipeline de integración tiene que tardar menos de 10 minutos, y las herramientas tienen que ser gratuitas para un repositorio público.

## Decisión

| Control | Herramienta | Dónde corre | Umbral que detiene el pipeline | Mínimo del enunciado |
|---|---|---|---|---|
| SAST | **CodeQL** (`security-extended`, modo sin compilación) + `sarif_gate.py` | `ci.yml` → *Seguridad - SAST* | Cualquier hallazgo con `security-severity` ≥ 7.0 (alto o crítico) que no esté tratado | Ningún alto o crítico sin tratar |
| SCA | **Trivy fs** sobre el `pom.xml` resuelto + **dependency-review-action** en los PR | `ci.yml` → *Seguridad - dependencias* y *revisión de dependencias del PR* | Cualquier CVE **alto o crítico** con corrección disponible | Igual |
| Secretos | **Gitleaks** sobre **todo el historial** (`fetch-depth: 0`) | `ci.yml` → *Seguridad - detección de secretos* | **Un** hallazgo | Cero secretos, incluido el historial |
| Imagen | **Trivy image** sobre la imagen recién construida | `ci.yml` → *Imagen Docker y escaneo* | Cualquier CVE **alto o crítico** con corrección disponible | Solo crítico con corrección: **endurecido** |
| DAST (bonificación) | **OWASP ZAP baseline** contra la URL desplegada | `cd.yml` → *Seguridad - DAST* | Cualquier alerta WARN o FAIL no tratada en `.zap/rules.tsv` | — |

### Por qué estas herramientas

**SAST.** Se eligió CodeQL sobre Semgrep y SonarCloud:

- es gratuito para repositorios públicos;
- se integra con la pestaña Security;
- el conjunto `security-extended` cubre inyección, deserialización insegura, criptografía débil y *log injection* en Java;
- el modo `build-mode: none` analiza sin compilar, lo que ahorra unos dos minutos.

CodeQL **solo reporta**, así que se agregó `.github/scripts/sarif_gate.py`. El script lee el SARIF, toma la `security-severity` de cada regla y falla el job si queda un hallazgo por encima del umbral que no esté en `.github/security/sast-allowlist.txt`. Esa lista está vacía, y cada entrada necesita su justificación en `docs/seguridad.md`. SonarCloud pedía una cuenta y un token externos. Semgrep habría duplicado el análisis sin cubrir nada que CodeQL no cubra en Java.

**SCA.** Se eligió Trivy sobre OWASP Dependency-Check porque es más rápido: menos de un minuto frente a varios minutos con la base del NVD, y no necesita una llave de la API del NVD. Además es la misma herramienta del escaneo de imagen, así que el equipo aprende una sola. `dependency-review-action` agrega un segundo filtro en el PR: bloquea la dependencia nueva **antes** de que llegue a main y comenta el motivo en el PR. Dependabot (`.github/dependabot.yml`) abre los PR de actualización, pero no es un control: solo avisa.

**Secretos.** Se eligió Gitleaks sobre TruffleHog por velocidad. Además se ejecuta el binario oficial, sin la *action* que pide licencia en organizaciones. Revisa el historial completo y no solo el último commit, porque un secreto borrado en un commit posterior sigue en el historial.

**Imagen.** Trivy con `--ignore-unfixed`: una vulnerabilidad sin corrección publicada no se puede arreglar actualizando, así que bloquear por ella detendría el pipeline sin que el equipo pueda hacer nada. Ese caso se registra como riesgo aceptado. Para que los parches del sistema operativo estén al día, el Dockerfile hace `apt-get upgrade` al construir.

### Por qué esos umbrales

- **Alto y crítico, no medio.** Con medio, una sola dependencia transitiva de Jetty puede tener hallazgos de bajo impacto que no afectan este uso (por ejemplo, en módulos que no se cargan). El equipo pasaría el corte justificando falsos positivos en vez de corregir lo importante.
- **Imagen endurecida a alto.** El enunciado pide solo crítico. Se subió a alto porque la imagen base es Ubuntu, que publica parches con frecuencia, y el `apt-get upgrade` los aplica. Subir el umbral casi no cuesta.
- **Cero secretos.** Un secreto filtrado no tiene grados: se rota y punto.

### Cómo se trata un hallazgo

Cada hallazgo se registra en [docs/seguridad.md](../seguridad.md) con una de tres decisiones: **corregido** (con el enlace al commit), **falso positivo** (con la justificación técnica) o **riesgo aceptado** (con la justificación y lo que haría falta para corregirlo). Solo después de registrarlo puede entrar en `.trivyignore`, `sast-allowlist.txt` o `.zap/rules.tsv`.

Desactivar una regla o bajar un umbral sin ese registro cuenta como un control no implementado, y la revisión del PR lo rechaza.

## Consecuencias

- **Tiempo.** Los controles corren en paralelo después de la construcción. El más lento es CodeQL, con unos 3 minutos, y la CI completa queda por debajo de 10 minutos con la caché de Maven.
- **Evidencia.** Hay un PR que introduce a propósito una dependencia vulnerable y una llave falsa. El pipeline lo bloquea y se cierra sin fusionar. Está enlazado en `docs/seguridad.md`.
- **Fuera de alcance.** Ninguna de estas herramientas detecta errores de lógica, como el teletransporte o la suplantación del `playerId`. Esos los cubren el modelo STRIDE y las pruebas de integración de seguridad (`MultijugadorIT`, pruebas A-01 a A-05), que también detienen el pipeline.
