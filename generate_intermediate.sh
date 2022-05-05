#!/usr/bin/env bash
orig_pwd="$PWD"
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
if [[ -v "MYARGS[threads]" ]]; then
    threads="${MYARGS[threads]}"
else
    threads=1
fi
if [[ -v "MYARGS[incorrect-count]" ]]; then
    incorrect_count="${MYARGS[incorrect-count]}"
else
    incorrect_count=5
fi
# ResourceRankConfidence uses much more memory than other scripts.
# This value may need to be changed depending on how much memory your system has.
readonly RESOURCE_RANK_THREADS=2
cd "$DIR"
python get_triples/process_triples.py --project-root "$orig_pwd" --properties-file "${MYARGS[config]}" --data-out "${MYARGS[intermediate-dir]}" --incorrect-count "$incorrect_count"
python SearchPaths2.py -t "$threads" --file-data "${MYARGS[intermediate-dir]}"
python search.py -t "$threads" --file-data "${MYARGS[intermediate-dir]}"
python pagerank.py -t "$threads" --file-data "${MYARGS[intermediate-dir]}"
python ResourceRankConfidence.py -t "$RESOURCE_RANK_THREADS" --file-data "${MYARGS[intermediate-dir]}"
