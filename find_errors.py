import argparse
import os
import sys

import pandas as pd

try:
    from IPython import embed
except ImportError:
    pass

def read_2id_file(path):
    with open(path, "r") as _2id_file:
        return {int(v): k for (k, v) in (l.rstrip().split() for l in _2id_file)}

def unmap_dataset(df, ent_map, rel_map):
    new_df = df[[0, 2, 1] + list(range(3, len(df.columns)))]
    #embed()
    new_df[0] = new_df[0].map(ent_map)
    new_df[1] = new_df[1].map(ent_map)
    new_df[2] = new_df[2].map(rel_map)
    return new_df




if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir", required=True)
    parser.add_argument("--confidence-file")
    parser.add_argument("--test-file")
    parser.add_argument("--threshold",type=float)
    parser.add_argument("--top",type=int)
    args = parser.parse_args()
    if args.threshold is None and args.top is None:
        print("Must supply threshold or top.")
        sys.exit(1)
    if not args.confidence_file:
        test_conf = os.path.join(args.data_dir, "result/Model1_model_TransE-test---train_conf1.txt")
        valid_conf = os.path.join(args.data_dir, "result/Model1_model_TransE-valid---train_conf1.txt")
        if os.path.exists(test_conf):
            if os.path.exists(valid_conf):
                print(f"Both {test_conf} and {valid_conf} present.\nYou must use --confidence-file to specify which to use.")
            else:
                args.confidence_file = test_conf
        else:
            args.confidence_file = valid_conf
    if not args.test_file:
        args.test_file = os.path.join(args.data_dir, "valid2id.txt")
    id2entity = read_2id_file(os.path.join(args.data_dir, "entity2id.txt"))
    id2rel = read_2id_file(os.path.join(args.data_dir, "relation2id.txt"))
    test_triples = unmap_dataset(pd.read_csv(args.test_file, sep=" ", header=None), id2entity, id2rel)
    #embed()
    if args.threshold is not None:
        with open(args.confidence_file, "r") as confidence_file:
            for row, conf in zip(test_triples.itertuples(index=None,name=None), confidence_file):
                conf = float(conf.rstrip())
                if conf < args.threshold:
                    print(" ".join(row))
    else:
        conflist = []
        with open(args.confidence_file, "r") as confidence_file:
            for row, conf in zip(test_triples.itertuples(index=None,name=None), confidence_file):
                conf = float(conf.rstrip())
                conflist.append((conf, row))
        for conf, row in sorted(conflist)[:args.top]:
            print(conf, row)
    
