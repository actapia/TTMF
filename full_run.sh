#!/usr/bin/env bash
#set -x
DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
source optionsh.sh
declare -A MYARGS
parse_args $0 MYARGS "$@"
parse_result=$?
if [ $parse_result -ne 0 ]; then
    if [ $parse_result -eq 101 ]; then
	exit 0
    else
	exit $parse_result
    fi
fi
set -e
if [[ -v "MYARGS[processes]" ]]; then
    threads="${MYARGS[processes]}"
else
    threads=1
fi
if [[ -v "MYARGS[incorrect-count]" ]]; then
    incorrect_count="${MYARGS[incorrect-count]}"
else
    incorrect_count=5
fi
TRAIN_FILE_PATH="$(realpath "${MYARGS[train-file]}")"
DEV_FILE_PATH="$(realpath "${MYARGS[dev-file]}")"
TEST_FILE_PATH="$(realpath "${MYARGS[test-file]}")"
intermed="$(realpath "${MYARGS[intermediate-dir]}")"
if ! [ -d "$intermed" ]; then
    mkdir "$intermed"
fi
cd "$DIR"
bash compute_embeddings.sh -t "$TRAIN_FILE_PATH" -d "$DEV_FILE_PATH" -T "$TEST_FILE_PATH" -i "$intermed"
bash generate_intermediate.sh -t "$threads" -c "${intermed}/KGE_config.properties" -I "$incorrect_count" -i "$intermed"
python Model1.py --file-data "$intermed" --test test

