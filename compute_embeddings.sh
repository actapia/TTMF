#!/usr/bin/env bash
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
# shellcheck disable=SC2155
export TRAIN_FILE_PATH="$(realpath "${MYARGS[train-file]}")"
# shellcheck disable=SC2155
export DEV_FILE_PATH="$(realpath "${MYARGS[dev-file]}")"
# shellcheck disable=SC2155
export TEST_FILE_PATH="$(realpath "${MYARGS[test-file]}")"
dev_filename="${DEV_FILE_PATH%.*}"
dev_extension="${DEV_FILE_PATH##*.}"
export TC_DEV_FILE_PATH="${dev_filename}_tc.${dev_extension}"
test_filename="${TEST_FILE_PATH%.*}"
test_extension="${TEST_FILE_PATH##*.}"
export TC_TEST_FILE_PATH="${test_filename}_tc.${test_extension}"
intermed="$(realpath "${MYARGS[intermediate-dir]}")"
config_path="${intermed}/KGE_config.properties"
envsubst < "$DIR/KGE_config_template.properties" > "$config_path"
cd "$DIR/KGE"
mvn clean package
java -jar target/Data-jar-with-dependencies.jar -c "$config_path"
java -jar target/TransE-jar-with-dependencies.jar -c "$config_path" -O "$intermed"
java -jar target/PTransE-jar-with-dependencies.jar -c "$config_path" -O "$intermed"
