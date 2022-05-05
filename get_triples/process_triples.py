import pandas as pd
import os
import argparse
try:
    from IPython import embed
except ImportError:
    pass
from collections import Counter

import numpy as np

def read_dataset(path):
    return pd.read_csv(path, sep="\t", header=None)

def read_datasets_from_properties(project_root, props, names):
    return [read_dataset(os.path.join(project_root, props[name])) for name in names]

def map_dataset(df, ent_map, rel_map):
    new_df = df[[0, 2, 1] + list(range(3, len(df.columns)))]
    #embed()
    new_df[0] = new_df[0].map(ent_map)
    new_df[2] = new_df[2].map(ent_map)
    new_df[1] = new_df[1].map(rel_map)
    return new_df

def select_tuples(df, cols):
    return Counter(df[cols].itertuples(index=False, name=None))

def counter_threshold(counter, threshold):
    yield from ((c, v) for (c, v) in counter.items() if v >= threshold)

def leave_target_out(target):
    return list(i for i in range(3) if i != target)

def gen_negative_examples(df, target, incorrect_count, value_map):
    ix = leave_target_out(target)
    df = df.groupby(ix).first()
    #embed()
    df = df.reset_index()
    print(incorrect_count)
    print(len(value_map))
    # np.random.randint(
    #     0,
    #     len(value_map),
    #     incorrect_count
    # )
    print(ix)
    #embed()
    res = df.groupby(ix).apply(
        lambda g: pd.DataFrame(
            {
                target: list(
                    np.random.randint(
                        0,
                        len(value_map),
                        incorrect_count
                    )
                ) + [g[target].iloc[0]]
            }
        )
    ).reset_index()[[0,2,1]]
    res[3] = 1
    return res

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--properties-file", required=True)
    parser.add_argument("--project-root", required=False)
    parser.add_argument("--data-out", required=True)
    parser.add_argument("--incorrect-count", type=int, required=True)
    args = parser.parse_args()
    if not args.project_root:
        args.project_root = os.path.dirname(args.properties_file)
    props = {}
    with open(args.properties_file, "r") as prop_file:
        props = {split[0]: split[1] for split in [l.rstrip().split("=") for l in prop_file]}
    train, dev, test, dev_tc, test_tc = read_datasets_from_properties(
        args.project_root,
        props,
        [s + "_FILE_PATH" for s in ["TRAIN", "DEV", "TEST", "TC_DEV", "TC_TEST"]]
    )
    # Generate ID mapppings.
    ent_map = {v: i for i, v in enumerate(set(train[0].unique()) | set(train[2].unique()))}
    rel_map = {v: i for i, v in enumerate(set(train[1].unique()))}
    
    #embed()
    train2id = map_dataset(train, ent_map, rel_map)
    valid2id = map_dataset(dev, ent_map, rel_map)
    test2id = map_dataset(test, ent_map, rel_map)

    conf_valid2id = map_dataset(dev_tc, ent_map, rel_map)
    conf_test2id = map_dataset(test_tc, ent_map, rel_map)

    # Produce data for hr_, h_t, _rt files.
    # unique_hr_ = select_tuples(test, [0, 1])
    # unique_h_t = select_tuples(test, [0, 2])
    # unique__rt = select_tuples(test, [1, 2])

    hr_ = gen_negative_examples(test2id, 2, args.incorrect_count, ent_map)
    h_t = gen_negative_examples(test2id, 1, args.incorrect_count, rel_map)
    _rt = gen_negative_examples(test2id, 0, args.incorrect_count, ent_map)

    #embed()

    with open(os.path.join(args.data_out, "entity2id.txt"), "w") as entity2id_file:
        for v, i in ent_map.items():
            entity2id_file.write("{}\t{}\n".format(v, i))

    with open(os.path.join(args.data_out, "relation2id.txt"), "w") as relation2id_file:
        for v, i in rel_map.items():
            relation2id_file.write("{}\t{}\n".format(v, i))

    train2id.to_csv(os.path.join(args.data_out, "train2id.txt"), index=False, sep=" ", header=None)
    valid2id.to_csv(os.path.join(args.data_out, "valid2id.txt"), index=False, sep=" ", header=None)
    test2id.to_csv(os.path.join(args.data_out, "test2id.txt"), index=False, sep=" ", header=None)

    conf_valid2id.to_csv(os.path.join(args.data_out, "conf_valid2id.txt"), index=False, sep="\t", header=None)
    conf_test2id.to_csv(os.path.join(args.data_out, "conf_test2id.txt"), index=False, sep="\t", header=None)

    hr_.to_csv(os.path.join(args.data_out, "hr_.txt"), index=False, sep="\t", header=None)
    h_t.to_csv(os.path.join(args.data_out, "h_t.txt"), index=False, sep="\t", header=None)
    _rt.to_csv(os.path.join(args.data_out, "_rt.txt"), index=False, sep="\t", header=None)
    
    #embed()
    
