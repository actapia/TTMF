# Triple Trustworthiness Measurement for Knowledge Graph

This is a fork of the original [Triple Trustworthiness Measurement for Knowledge Graph](https://github.com/TJUNLP/TTMF) repository with various improvements. I have included in this repository additional code for computing embeddings originally provided only through a separate download as well as various convenience scripts for performing pre-processing and running the full TTMF pipelines. I hope you find these useful.

This README is specifically for this fork of TTMF. You can find the original README file [here](README.md).

## Requirements

This software has been tested with the configuration below. Similar configurations likely also work but are untested.

* Python 3.10.4
* Keras 2.7.0
* more-itertools 8.12.0
* pandas 1.4.2
* scikit-learn 1.0.2
* scipy 1.8.0
* tqdm 4.64.0
* [python-graph](https://github.com/Shoobx/python-graph/) afd6f1c (Don't confuse this with pygraph.)
* Bash 5.1.8
* [optionsh](https://github.com/actapia/optionsh)
* Java 11.0.15 (OpenJDK 11.0.15)
* Maven 3.6.3

## Usage

### Obtaining data
 
Obtain your data in a format in which each triple occupies a single line, and the three elements of each triple are separted by tabs. For example,

```
http://dbpedia.org/resource/Ada_Lovelace	http://dbpedia.org/ontology/birthPlace	http://dbpedia.org/resource/London
http://dbpedia.org/resource/Charles_Babbage	http://dbpedia.org/ontology/influenced	http://dbpedia.org/resource/Ada_Lovelace
http://dbpedia.org/resource/Drummond_Bone	http://dbpedia.org/ontology/profession	http://dbpedia.org/resource/Lord_Byron
```

### Splitting data into train and test sets

Next, split the data into train, validation, and test sets. Importantly, the test and validation sets must include only entities and relations present in the training set. If your data is already split this way, you can skip this step.

Alternatively, you can split the data this using the `get_triples/train_test_split.py` script. The script requires that you specify the total number of triples across all three sets, the ratio between the training set size and the combined test and validation set sizes, and the ratio between the testing set size and the development set size.

```bash
python train_test_split.py --total-size 500000 --train-ratio 0.8 --dev-ratio 0.5
```

### Running TTMF

You can run the full TTMF pipeline using the `full_run.sh` script.

Place your train, validation, and test sets in a location to which you have write access; this script will attempt to create new files in the same directory.

Some steps of this process can be time-consuming. If you are using a machine with multiple cores, you can speed up computation dramatically by providing the `--processes`/`-p` option to this script to specify the number of processes to use. Multiple cores will be used for most parts of the pipeline.

`ResourceRankConfidence.py` can be run in parallel as well, but each process requires a relatively large amount of memory. The memory usage per process seems to be roughly 32 GB per 500,000 triples. Hence, if you 64 GB of memory, you can use two processes for `ResourceRankConfidence.py`. By default, this step runs with just one process. You can change the number of processes by editing the `RESOURCE_RANK_THREADS` constant in the `generate_intermediate.sh` script.

You must also specify a directory in which to store intermediate files. Optionally, you can specify a count of the number of incorrect triples to generate for each correct triple when generating the <h, r, _>, <h, _, t>, and <_, r, t> triples.

```bash
bash full_run.sh --train-file /path/to/train/file.txt \
                 --dev-file /path/to/valid/file.txt \
		 --test-file /path/to/valid/file.txt \
		 --intermediate-dir intermed \
		 --processes 28 \
		 --incorrect-count 5
```

The final results will be written to the `results` subdirectory of your specified `intermed` directory.

### Finding detected errors

You can find triples with low confidence scores using the `find_errors.py` script. You must specify a path to your intermediate directory or a file containing the confidence scores for positive testing examples.

To find triples with confidence below a given threshold, use a command like the following.

```bash
python find_errors.py --data-dir intermed --threshold 0.1
```

Alternatively, to get the k triples with lowest confidence, use a command like this.

```bash
bash find_errors.py --data-dir intermed --top 10
```

## Citation

The citation for the paper from the original repository is preserved below. Please cite this paper if you use this code.

Shengbin Jia, Yang Xiang, and Xiaojun Chen. 2019. Triple Trustworthiness Measurement for Knowledge Graph. In Proceedings of the 2019 World Wide Web Conference (WWW ’19), May 13–17, 2019, San Francisco, CA, USA. ACM, New York, NY, USA, 7 pages. 

Upstream contact: shengbinjia@tongji.edu.cn