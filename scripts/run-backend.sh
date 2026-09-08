#!/usr/bin/env bash
# Runs the backend with mail credentials from backend/.env.local.
#
# Credentials come from a file rather than the command line: a password typed
# into a shell is kept in history and is visible in `ps` to other users.
set -euo pipefail

cd "$(dirname "$0")/.."
ENV_FILE="backend/.env.local"

if [ -f "$ENV_FILE" ]; then
  # PARSED, not sourced. `source` executes the file as shell, so an unquoted
  # value like  Doukyo <you@gmail.com>  is read as redirection and blows up —
  # and anything in backticks or $(...) would actually RUN. Splitting on the
  # first '=' keeps values literal, including passwords containing '=' or spaces.
  while IFS='=' read -r key value || [ -n "$key" ]; do
    key="${key%%$'\r'}"; value="${value%%$'\r'}"          # tolerate CRLF
    [ -z "$key" ] && continue
    case "$key" in \#*) continue ;; esac                   # skip comments
    value="${value#\"}"; value="${value%\"}"               # strip optional quotes
    value="${value#\'}"; value="${value%\'}"
    export "$key=$value"
  done < "$ENV_FILE"

  : "${SPRING_MAIL_HOST:=}" "${DOUKYO_MAIL_FROM:=}"
  if [ -z "$SPRING_MAIL_HOST" ] || [ -z "$DOUKYO_MAIL_FROM" ]; then
    echo "WARNING: $ENV_FILE is missing SPRING_MAIL_HOST or DOUKYO_MAIL_FROM."
    echo "         Mail will be DISABLED and codes printed to this log."
    echo "         Compare against backend/.env.example."
  else
    echo "Mail: sending as $DOUKYO_MAIL_FROM via $SPRING_MAIL_HOST"
  fi
else
  echo "No $ENV_FILE — starting without mail; codes will be printed to this log."
  echo "Copy backend/.env.example to $ENV_FILE and fill it in to send real email."
fi

cd backend
exec ./gradlew bootRun
