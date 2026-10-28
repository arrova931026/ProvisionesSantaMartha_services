# Configurar notificaciones de WhatsApp

## Qué envía la aplicación

Spring Boot revisa diariamente los cobros de contratos vigentes y envía al teléfono principal (`personas.telefono`):

- Recordatorio de pago próximo, por defecto 5 días antes de `fecha_limite`.
- Aviso de pago vencido, una sola vez por cuota vencida.

Cada aviso también se guarda en la bandeja interna. Se usa una referencia idempotente por cuota para evitar duplicados. Si Meta rechaza un recordatorio próximo, se reintenta en las ejecuciones diarias mientras la cuota esté dentro de los próximos cinco días y aún no venza.

El envío de WhatsApp está desactivado por defecto y solo se procesa para clientes que aceptaron recibir esos mensajes (`whatsapp_opt_in = true`). Los teléfonos de México de 10 dígitos se normalizan a `52` + 10 dígitos. También se aceptan números con prefijo `52` o el formato móvil antiguo `521`.

## 1. Preparar Meta WhatsApp Cloud API

1. En Meta for Developers crea o selecciona una aplicación de tipo Business y añade el producto **WhatsApp**.
2. Vincula una cuenta de WhatsApp Business y configura el número emisor.
3. En **WhatsApp > API Setup**, copia el **Phone Number ID**. No es el número telefónico visible.
4. Genera un token de acceso de sistema/permanente con los permisos `whatsapp_business_messaging` y `whatsapp_business_management`. Guarda el token como secreto; no lo agregues al repositorio.
5. Crea y espera la aprobación de dos plantillas de categoría **Utility**, idioma `Spanish (Mexico)` (`es_MX`):

   Nombre `shsm_pago_proximo`, cuerpo con cinco variables en este orden:
   `Hola {{1}}. Te recordamos que la cuota {{2}} del contrato {{3}} por {{4}} vence el {{5}}. Si ya realizaste el pago, puedes ignorar este mensaje.`

   Nombre `shsm_pago_vencido`, cuerpo con cinco variables en este orden:
   `Hola {{1}}. La cuota {{2}} del contrato {{3}} por {{4}} venció el {{5}}. Para mantener tu cobertura, por favor regulariza el pago. Si ya pagaste, ignora este aviso.`

   Las plantillas deben conservar exactamente cinco parámetros de cuerpo. Meta puede exigir ajustes durante su revisión.

Los recordatorios son mensajes iniciados por la empresa; por eso deben enviarse como plantillas aprobadas y solo a clientes con consentimiento previo. No se usa el envío de texto libre fuera de la ventana de atención de WhatsApp.

## 2. Aplicar migración de base de datos

Ejecuta una vez `database/09_whatsapp_notificaciones.sql` contra la misma base configurada para el backend. La migración crea el consentimiento `personas.whatsapp_opt_in` (inicialmente falso) y la referencia idempotente de notificaciones.

No actives consentimientos en bloque sin autorización documentada. Cada cliente puede activarlo o retirarlo desde **Notificaciones > Avisos por WhatsApp**.

## 3. Variables de entorno

Configura estas variables en el entorno del backend (por ejemplo, **Render > Service > Environment**):

| Variable | Obligatoria | Valor inicial / uso |
| --- | --- | --- |
| `WHATSAPP_ENABLED` | Sí para enviar | `true` |
| `WHATSAPP_PHONE_NUMBER_ID` | Sí | Phone Number ID de Meta |
| `WHATSAPP_ACCESS_TOKEN` | Sí | Token secreto de sistema; nunca lo subas a Git |
| `WHATSAPP_GRAPH_API_VERSION` | Sí | Versión Graph API vigente admitida por la app de Meta, por ejemplo `v23.0` |
| `WHATSAPP_TEMPLATE_LANGUAGE` | Sí | `es_MX` |
| `WHATSAPP_TEMPLATE_UPCOMING` | No | `shsm_pago_proximo` |
| `WHATSAPP_TEMPLATE_OVERDUE` | No | `shsm_pago_vencido` |
| `WHATSAPP_REMINDER_DAYS_BEFORE` | No | `5` |
| `WHATSAPP_REMINDER_CRON` | No | `0 0 10 * * *` (diario a las 10:00) |
| `WHATSAPP_TIME_ZONE` | No | `America/Mexico_City` |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | Para verificar webhook | Secreto aleatorio creado por ti; el mismo valor se escribe en Meta |
| `WHATSAPP_APP_SECRET` | Para validar POST de webhook | App Secret desde **Configuración de la app > Básica** |

En local, exporta las variables antes de iniciar Spring Boot. No incluyas el token en `application.yml`, colecciones Postman, logs ni commits.

## Callback durante desarrollo local

Meta no puede llamar a `localhost`. Expón temporalmente el puerto 8081 con un túnel HTTPS, por ejemplo:

```bash
ngrok http 8081
```

Ngrok mostrará una URL HTTPS, por ejemplo `https://abc123.ngrok-free.app`. En **Paso 2 > Configurar webhooks**, escribe como URI de devolución de llamada:

```text
https://abc123.ngrok-free.app/api/webhooks/whatsapp
```

El dominio es solo un ejemplo: usa exactamente el HTTPS que te entregue tu túnel. Genera un token de verificación independiente (no es el token de Meta):

```bash
openssl rand -hex 32
```

Copia ese resultado directamente en el campo **Token de verificación** de Meta y configura el mismo valor localmente como `WHATSAPP_WEBHOOK_VERIFY_TOKEN`. También configura `WHATSAPP_APP_SECRET` con el **App Secret** de Meta. No uses en esos campos el Access Token de WhatsApp ni el Phone Number ID.

El GET `/api/webhooks/whatsapp` valida `hub.verify_token` y devuelve el `hub.challenge`; el POST valida `X-Hub-Signature-256` usando el App Secret y responde 200. Actualmente el POST solo confirma la recepción; no procesa ni almacena estados de entrega. Para recordatorios salientes por plantilla, el webhook no es requisito funcional: se puede dejar sin configurar mientras pruebas el envío.

Los dominios gratuitos del túnel pueden cambiar al reiniciarlo. Si cambia, actualiza la URI en Meta. Para una URL estable usa un dominio/túnel reservado o despliega el backend con HTTPS público.

## 4. Validar

1. Reinicia Spring Boot para cargar la migración y las variables.
2. Inicia sesión como cliente y activa el interruptor de WhatsApp en Notificaciones. Verifica que el teléfono principal esté guardado en formato válido.
3. Revisa en Meta que ambas plantillas estén aprobadas y que el número emisor permita enviar al destinatario de prueba.
4. Para probar sin esperar al job, cambia temporalmente el cron en local o ejecuta el método del servicio desde una prueba de integración con un cobro cuya fecha límite sea la fecha esperada. No hagas pruebas contra clientes reales sin consentimiento.
5. Revisa logs del backend y `notificaciones.canal`, `enviada`, `fecha_envio`. `APP+WHATSAPP` y `enviada=true` indican respuesta HTTP exitosa de Meta; los errores no registran el token.

## Limitaciones

- La verificación de envío es la aceptación de la solicitud por Graph API; la entrega final también depende del estado del destinatario y del número en Meta.
- El backend debe ejecutarse en una sola réplica para evitar dos jobs simultáneos. La referencia única reduce duplicados en base de datos, pero el proveedor no ofrece exactamente-una-vez si la conexión se corta después de aceptar el mensaje.
- Por ahora se envían recordatorios de cuotas próximas y vencidas. La bandeja ya puede alojar otros tipos de notificación, pero cada evento adicional necesita definirse e integrarse con su plantilla correspondiente.