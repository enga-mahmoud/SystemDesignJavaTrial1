{{/*
Chart name
*/}}
{{- define "ecommerce.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Chart label value (name-version)
*/}}
{{- define "ecommerce.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to every resource
*/}}
{{- define "ecommerce.labels" -}}
helm.sh/chart: {{ include "ecommerce.chart" . }}
app.kubernetes.io/name: {{ include "ecommerce.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Resolve a container image reference.
Usage: {{ include "ecommerce.image" (dict "repo" .Values.userService.image.repository "tag" .Values.userService.image.tag "global" .Values.global) }}
Service-level tag takes priority over global.imageTag; falls back to "latest".
*/}}
{{- define "ecommerce.image" -}}
{{- $tag := .tag | default .global.imageTag | default "latest" -}}
{{- printf "%s:%s" .repo $tag -}}
{{- end }}

{{/*
Init container: waits until a TCP port is accepting connections.
Usage: {{ include "ecommerce.waitForPort" (dict "name" "postgres-user" "host" "postgres-user" "port" "5432") }}
*/}}
{{- define "ecommerce.waitForPort" -}}
- name: wait-for-{{ .name }}
  image: busybox:1.36
  command:
    - sh
    - -c
    - until nc -z {{ .host }} {{ .port }}; do echo "waiting for {{ .name }}"; sleep 3; done
{{- end }}

{{/*
Init container: waits until a Spring Boot /actuator/health endpoint returns UP.
Usage: {{ include "ecommerce.waitForHealth" (dict "name" "config-server" "url" "http://config-server:8888/actuator/health") }}
*/}}
{{- define "ecommerce.waitForHealth" -}}
- name: wait-for-{{ .name }}
  image: busybox:1.36
  command:
    - sh
    - -c
    - until wget -qO- {{ .url }} | grep -q UP; do echo "waiting for {{ .name }}"; sleep 5; done
{{- end }}

{{/*
Standard readiness probe for Spring Boot services.
Usage: {{ include "ecommerce.readinessProbe" (dict "port" 8081 "initialDelay" 40) }}
*/}}
{{- define "ecommerce.readinessProbe" -}}
readinessProbe:
  httpGet:
    path: /actuator/health
    port: {{ .port }}
  initialDelaySeconds: {{ .initialDelay }}
  periodSeconds: 10
  failureThreshold: 15
{{- end }}

{{/*
Standard liveness probe for Spring Boot services.
Usage: {{ include "ecommerce.livenessProbe" (dict "port" 8081 "initialDelay" 80) }}
*/}}
{{- define "ecommerce.livenessProbe" -}}
livenessProbe:
  httpGet:
    path: /actuator/health
    port: {{ .port }}
  initialDelaySeconds: {{ .initialDelay }}
  periodSeconds: 20
{{- end }}

{{/*
ConfigMap env var reference helper.
Usage: {{ include "ecommerce.configRef" "MY_KEY" }}
*/}}
{{- define "ecommerce.configRef" -}}
valueFrom:
  configMapKeyRef:
    name: app-config
    key: {{ . }}
{{- end }}
