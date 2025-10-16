#!/usr/bin/env bash
set -euo pipefail

# Amnesiac APK Signer
#
# Two subcommands:
#   - sign-and-forget: Generate a one-off keystore, sign the APK, verify it, then delete the keystore.
#   - verify: Print signer info of a signed APK. If not signed, prints nothing.
#
# Requirements (on PATH):
#   - apksigner (or apksigner.bat)
#   - keytool
#   - openssl (optional; improves randomness for passwords)
#
# Examples similar to your existing notes:
#   verify example: apksigner.bat verify --print-certs @androidapp/app/build/outputs/apk/debug/app-debug.apk
#   sign example:   apksigner.bat sign --ks @androidapp/keystore.jks @androidapp/app/build/outputs/apk/debug/app-debug.apk

script_name() { basename "$0"; }
# Enable bash tracing when DEBUG=1
if [[ ${DEBUG:-0} != 0 ]]; then set -x; fi

print_usage() {
  cat <<EOF
Usage: $(script_name) <command> [options]

Commands:
  sign-and-forget <apk> [-o <out.apk>]   Sign APK with an ephemeral key and delete the key.
  verify <apk>                            Print signer info if APK is signed; otherwise print nothing.

Options for sign-and-forget:
  -o, --out <file>      Output APK path. Defaults to <input>-signed.apk
  --alias <name>        Key alias to use (defaults to amnesiac-<timestamp>)
  --validity <days>     Validity in days (default: 10000)
  --dname <dn>          Distinguished name for the key (default: CN=Amnesiac,O=Ephemeral,C=US)

Environment:
  Relies on 'apksigner' (or 'apksigner.bat') and 'keytool' being available in PATH.
EOF
}

fail() { echo "[error] $*" >&2; exit 1; }
info() { echo "[info]  $*" >&2; }

detect_tools() {
  if command -v apksigner >/dev/null 2>&1; then
    APKSIGNER="apksigner"
  elif command -v apksigner.bat >/dev/null 2>&1; then
    APKSIGNER="apksigner.bat"
  else
    fail "'apksigner' not found in PATH"
  fi

  if command -v keytool >/dev/null 2>&1; then
    KEYTOOL="keytool"
  else
    fail "'keytool' not found in PATH"
  fi
}

rand_pass() {
  if command -v openssl >/dev/null 2>&1; then
    # 32 bytes base64 -> ~43 chars
    # Strip both \n and \r to avoid CRLF issues on Windows shells
    openssl rand -base64 32 | tr -d '\r\n'
  else
    # Fallback: combine date, PID, and random
    date +%s%N | sha1sum | cut -c1-40 2>/dev/null || echo "$RANDOM$RANDOM$$"
  fi
}

derive_out_path() {
  local in_apk="$1"
  local base ext
  base="${in_apk%.*}"
  ext="${in_apk##*.}"
  if [[ "$ext" == "apk" ]]; then
    echo "${base}-signed.apk"
  else
    echo "${in_apk}-signed.apk"
  fi
}

cmd_sign_and_forget() {
  local in_apk out_apk alias_name validity dname
  in_apk="${1:-}"
  shift || true
  [[ -n "$in_apk" ]] || fail "Missing APK path. See --help."
  [[ -f "$in_apk" ]] || fail "APK not found: $in_apk"

  out_apk=""
  alias_name="amnesiac-$(date +%s)"
  validity=10000
  dname="CN=Amnesiac,O=Ephemeral,C=US"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      -o|--out)
        out_apk="$2"; shift 2 ;;
      --alias)
        alias_name="$2"; shift 2 ;;
      --validity)
        validity="$2"; shift 2 ;;
      --dname)
        dname="$2"; shift 2 ;;
      --)
        shift; break ;;
      -h|--help)
        print_usage; exit 0 ;;
      *)
        fail "Unknown option for sign-and-forget: $1" ;;
    esac
  done

  [[ -n "$out_apk" ]] || out_apk="$(derive_out_path "$in_apk")"

  detect_tools
  info "Using APKSIGNER: $APKSIGNER"
  info "Using KEYTOOL:   $KEYTOOL"

  # Create ephemeral keystore in a temp dir
  local tmpdir keystore storepass keypass
  tmpdir=$(mktemp -d 2>/dev/null || mktemp -d -t amnesiac)
  keystore="$tmpdir/amnesiac-ks.p12"
  # Use a single random password for both store and key to avoid PKCS12 nuances
  keypass="$(rand_pass)"
  storepass="$keypass"
  # Ensure cleanup even if something fails (safe with set -u)
  trap 'ks=${keystore-}; td=${tmpdir-}; \
    if [[ -n "$ks" ]]; then rm -f "$ks" 2>/dev/null || true; fi; \
    if [[ -n "$td" ]]; then rm -rf "$td" 2>/dev/null || true; fi' EXIT

  # Convert paths for Windows Java/.bat tools when available
  KEYS_WIN="$keystore"
  IN_APK_WIN="$in_apk"
  OUT_APK_WIN="$out_apk"
  if command -v cygpath >/dev/null 2>&1; then
    KEYS_WIN=$(cygpath -w "$keystore")
    IN_APK_WIN=$(cygpath -w "$in_apk")
    OUT_APK_WIN=$(cygpath -w "$out_apk")
  fi

  info "Generating ephemeral keystore..."
  set +e
  "$KEYTOOL" -genkeypair -v \
    -noprompt \
    -storetype PKCS12 \
    -keystore "$KEYS_WIN" \
    -storepass "$storepass" \
    -keypass "$keypass" \
    -alias "$alias_name" \
    -keyalg RSA -keysize 2048 \
    -validity "$validity" \
    -dname "$dname"
  kt_ec=$?
  set -e
  [[ $kt_ec -eq 0 ]] || fail "keytool failed creating keystore at $KEYS_WIN"

  # Ensure output directory exists if --out points to a new location
  if [[ -n "$out_apk" ]]; then
    mkdir -p "$(dirname "$out_apk")" 2>/dev/null || true
  fi
  info "Signing APK -> $out_apk"
  if [[ "$APKSIGNER" == *".bat" ]]; then
    "$APKSIGNER" sign \
      --ks-type PKCS12 \
      --ks "$KEYS_WIN" \
      --ks-key-alias "$alias_name" \
      --ks-pass "pass:$keypass" \
      --key-pass "pass:$keypass" \
      --out "$OUT_APK_WIN" \
      "$IN_APK_WIN"
  else
    "$APKSIGNER" sign \
      --ks-type PKCS12 \
      --ks "$keystore" \
      --ks-key-alias "$alias_name" \
      --ks-pass "pass:$keypass" \
      --key-pass "pass:$keypass" \
      --out "$out_apk" \
      "$in_apk"
  fi

  info "Verifying signature..."
  # Show signer info; will also fail the script if invalid
  "$APKSIGNER" verify --print-certs "$out_apk"

  info "Deleting ephemeral keystore..."
  rm -f "$keystore" || true
  rm -rf "$tmpdir" || true

  info "Done. Signed APK: $out_apk"
}

cmd_verify() {
  local in_apk
  in_apk="${1:-}"
  [[ -n "$in_apk" ]] || fail "Missing APK path. See --help."
  [[ -f "$in_apk" ]] || fail "APK not found: $in_apk"
  detect_tools

  # If signed, print signer info to stdout. If not, print nothing.
  local output path_arg
  path_arg="$in_apk"
  if [[ "$APKSIGNER" == *".bat" ]] && command -v cygpath >/dev/null 2>&1; then
    path_arg=$(cygpath -w "$in_apk")
  fi
  output=$("$APKSIGNER" verify --print-certs "$path_arg" 2>/dev/null || true)
  if [[ -n "$output" ]]; then
    printf "%s\n" "$output"
  else
    # No output (not signed). Return non-zero to allow caller to branch if desired.
    exit 1
  fi
}

main() {
  if [[ $# -lt 1 ]]; then
    print_usage
    exit 1
  fi
  case "$1" in
    sign-and-forget)
      shift
      cmd_sign_and_forget "$@" ;;
    verify)
      shift
      cmd_verify "$@" ;;
    -h|--help|help)
      print_usage ;;
    *)
      fail "Unknown command: $1" ;;
  esac
}

main "$@"
