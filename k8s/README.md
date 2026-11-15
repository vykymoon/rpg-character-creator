# Kubernetes (bonificación del Corte 3)

Manifiestos para correr el servidor de partida en un clúster. El workflow [`k8s.yml`](../.github/workflows/k8s.yml) los despliega en **kind** en cada cambio y verifica:

- el rollout con las probes;
- el healthcheck a través del Service;
- la prueba de humo del flujo principal;
- que las NetworkPolicy se cumplan.

| Archivo | Qué es | Por qué |
|---|---|---|
| `namespace.yaml` | Namespace `rpg` con Pod Security `restricted` | Rechaza pods privilegiados o que corran como root |
| `configmap.yaml` | `PORT`, `MAX_PLAYERS`, límites anti-flood | Configuración no sensible, los mismos nombres de `.env.example` |
| `secret.yaml` | `JOIN_CODE` de **ejemplo** | En un clúster real se crea con `kubectl create secret` (abajo); el workflow lo reemplaza por un valor aleatorio |
| `rbac.yaml` | ServiceAccount `rpg-servidor` sin roles y sin token montado | **RBAC justificado:** el servidor no usa la API de Kubernetes. Sin token, un contenedor comprometido no puede listar secretos ni crear pods (amenaza A-08) |
| `deployment.yaml` | 1 réplica, `Recreate`, probes de startup, readiness y liveness sobre `/health`, *requests* 100m/256Mi y *limits* 500m/512Mi, sin root, sistema de archivos de solo lectura, `drop ALL` | Una réplica porque el estado de la partida está en memoria (ADR-001): dos réplicas serían dos partidas. Los límites salen de la imagen (la JVM usa el 75 % de la memoria) y de la carga medida |
| `service.yaml` | ClusterIP en el 8887 | HTTP y WebSocket en el mismo puerto (ADR-006) |
| `networkpolicy.yaml` | `denegar-todo` + entrada solo al 8887 + **ninguna salida** | **NetworkPolicy justificada:** el servidor no consume nada externo. Bloquear la salida impide exfiltrar datos o descargar herramientas si alguien ejecuta código en el pod (A-08), y solo el puerto del juego queda expuesto (A-05) |

## Desplegar a mano en kind o minikube

```bash
docker build -t rpg-servidor:k8s --build-arg APP_VERSION=k8s .
kind create cluster --name rpg                       # o: minikube start
kind load docker-image rpg-servidor:k8s --name rpg   # o: minikube image load rpg-servidor:k8s
kubectl apply -k k8s/
kubectl -n rpg create secret generic rpg-servidor-secret \
  --from-literal=JOIN_CODE="$(openssl rand -hex 10)" --dry-run=client -o yaml | kubectl apply -f -
kubectl -n rpg rollout restart deployment/rpg-servidor
kubectl -n rpg rollout status deployment/rpg-servidor
kubectl -n rpg port-forward svc/rpg-servidor 8887:8887
curl http://localhost:8887/health
```

**Resultado esperado:** `deployment "rpg-servidor" successfully rolled out` y `{"status":"UP","version":"k8s",...}`.

Para usar la imagen publicada en lugar de una local, cambiar `image:` en `deployment.yaml` por `ghcr.io/vykymoon/rpg-servidor:<version>`.
