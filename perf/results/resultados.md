# Resultados de las pruebas de carga

Mediciones del equipo sobre el servidor LAN. Escenarios oficiales del ADR-001, barrido de jugadores, barrido de frecuencia de envío y el efecto de desactivar el algoritmo de Nagle.

SLO: p95 ≤ 150 ms y error < 1 %.

## Escenarios oficiales

| Escenario | Carga | p95 | Errores | Veredicto |
|---|---|---|---|---|
| oficial-baseline | 2 jugadores, 200 ms | 3 ms | 0 % | Cumple con amplio margen |
| oficial-carga | 8 jugadores, 200 ms, 3 min | 43 ms | 0 % | Cumple |

## Barrido de jugadores (intervalo fijo de 200 ms)

| Escenario | Jugadores | p95 | Errores | Observación |
|---|---|---|---|---|
| jug-2 | 2 | 4 ms | 0 % | Sano |
| jug-4 | 4 | 45 ms | 0 % | Salto notorio frente a jug-2 |
| jug-8 | 8 | 47 ms | 0 % | Estable |
| jug-16 | 16 | 49 ms | 0 % | Estable |
| jug-24 | 24 | 51 ms | 0 % | Estable |
| jug-32 | 32 | 55 ms | 0 % | Último punto sano |
| jug-48 | 48 | 689 ms | 0,15 % | 3236 mensajes omitidos; empieza el colapso. CPU al 32 % |
| jug-64 | 64 | 1534 ms | 8,75 % | Rompe el SLO; throughput estancado en ~45.500 KB/s |

## Barrido de frecuencia

| Escenario | Jugadores | Intervalo | p95 | Omitidos |
|---|---|---|---|---|
| int8-50 | 8 | 50 ms | 48 ms | 38 |
| int8-100 | 8 | 100 ms | 46 ms | 0 |
| int8-200 | 8 | 200 ms | 48 ms | 0 |
| int8-500 | 8 | 500 ms | 47 ms | 0 |
| int8-1000 | 8 | 1000 ms | 47 ms | 0 |
| int32-100 | 32 | 100 ms | 100 ms | 592 |
| int32-500 | 32 | 500 ms | 64 ms | 0 |
| int32-1000 | 32 | 1000 ms | 58 ms | 0 |

## Efecto de TCP_NODELAY

| Escenario | Jugadores | p95 con Nagle | p95 sin Nagle |
|---|---|---|---|
| nodelay-2 | 2 | 4 ms | 4 ms |
| nodelay-4 | 4 | 45 ms | 3 ms |
| nodelay-8 | 8 | 47 ms | 5 ms |
| nodelay-16 | 16 | 49 ms | 6 ms |
| nodelay-32 | 32 | 55 ms | 9 ms |
| nodelay-48 | 48 | 689 ms | 739 ms (error 0,10 %) |
| nodelay-64 | 64 | 1534 ms | 1551 ms (error 9,47 %) |

## Qué sale de esto

**El SLO se cumple en la carga esperada.** Con 8 jugadores el p95 es 43 ms contra un límite de 150 ms, sin errores.

**El sistema aguanta hasta 32 jugadores y se cae en 48.** A partir de ahí el p95 pasa de 55 ms a 689 ms y aparecen errores.

**El cuello de botella no es la CPU.** En el colapso la CPU del servidor está al 32 %. Lo que se estanca es el throughput de red, en un techo de ~45.500 KB/s. El `broadcast` envía el estado completo a los N jugadores en cada movimiento, así que el tráfico crece con el cuadrado de los jugadores.

**El límite real son los paquetes por segundo, no los jugadores.** 32 jugadores a 100 ms llegan al mismo techo que 48 a 200 ms. Bajar la frecuencia a 500 ms devuelve el sistema a zona sana con los mismos 32 jugadores.

**Los 45 ms de latencia en estado sano eran del sistema operativo, no del código.** Desactivar el algoritmo de Nagle baja el p95 de 45–55 ms a 3–9 ms en todos los escenarios hasta 32 jugadores. El retraso venía de que el sistema operativo agrupaba paquetes pequeños antes de enviarlos.

**Pero TCP_NODELAY no salva del colapso.** Con 48 y 64 jugadores el resultado es el mismo con y sin Nagle. Confirma que el techo de ~45 jugadores es estructural, del modelo de difusión, y no un problema de configuración del socket.
