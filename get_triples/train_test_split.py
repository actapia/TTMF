import pandas
import sklearn.model_selection
import argparse
import os

try:
    from IPython import embed
except ImportError:
    pass

# def fair_train_test_split(df, ratio, number):
#     ids = [set() for _ in ratio]
#     #embed()

def replace_with_unk(df, col, uniq, tok):
    df[col][~df[col].isin(uniq)] = tok

def triple_is_in(df, uniq_ent, uniq_rel):
    return df[df[0].isin(uniq_ent) & df[2].isin(uniq_ent) & (df[1].isin(uniq_rel))]

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("dataset")
    parser.add_argument("--total-size", type=int, required=True)
    parser.add_argument("--train-ratio", type=float, required=True)
    parser.add_argument("--dev-ratio", type=float, required=True)
    args = parser.parse_args()
    csv = pandas.read_csv(args.dataset, sep="\t", header=None)
    original_csv = csv
    #embed()
    # if args.size:
    #     csv, _ = sklearn.model_selection.train_test_split(csv, train_size = args.size, random_state=628)
    train, test = sklearn.model_selection.train_test_split(csv, train_size = int(args.total_size * args.train_ratio), random_state=628)
    uniq_ent = set(train[0].unique()) | set(train[2].unique())
    uniq_rel = set(train[1].unique())
    test = triple_is_in(test, uniq_ent, uniq_rel)
    #embed()
    if len(test) > args.total_size:
        test, _ = sklearn.model_selection.train_test_split(test, train_size = int(args.total_size * (1-args.train_ratio)), random_state=628)
    dev, test = sklearn.model_selection.train_test_split(test, train_size = args.dev_ratio, random_state=628)
    
    # dev, test = sklearn.model_selection.train_test_split(test, train_size = 0.5, random_state=628)
    # dev = triple_is_in(dev, uniq_ent, uniq_rel)
    # test = triple_is_in(test, uniq_ent, uniq_rel)
    replace_with_unk(csv, 0, uniq_ent, "UNKENT")
    replace_with_unk(csv, 1, uniq_rel, "UNKREL")
    replace_with_unk(csv, 2, uniq_ent, "UNKENT")
    train = pandas.concat([train, pandas.DataFrame([["UNKENT", "UNKREL", "UNKENT"]])])
    # print("Replacing")
    # for i, df in enumerate([dev, test, csv]):
    #     print(i)
    #     replace_with_unk(df, 0, uniq_ent, "UNKENT")
    #     replace_with_unk(df, 1, uniq_rel, "UNKREL")
    #     replace_with_unk(df, 2, uniq_ent, "UNKENT")
    # print("New train")
    # train = pandas.concat([train, pandas.DataFrame([["UNKENT", "UNKREL", "UNKENT"]])])
    #embed()
    filename, extension = os.path.splitext(args.dataset)
    train.to_csv("{}_train{}".format(filename, extension), index=False, sep="\t", header=None)
    dev.to_csv("{}_dev{}".format(filename, extension), index=False, sep="\t", header=None)
    test.to_csv("{}_test{}".format(filename, extension), index=False, sep="\t", header=None)
    original_csv.to_csv("{}_unk{}".format(filename, extension), index=False, sep="\t", header=None)
