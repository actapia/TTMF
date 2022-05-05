#coding = utf-8

import time
from pygraph.classes.digraph import digraph
import os

import argparse
import concurrent.futures
from more_itertools import chunked

from tqdm import tqdm


def ReadAllTriples(files):
    dict = {}

    for f in files:
        file = open(f, "r")
        for line in file:
            list = line.split(" ")

            if list[0] in dict.keys():
                if list[1] in dict.get(list[0]).keys():
                    dict.get(list[0]).get(list[1]).append(list[2].strip('\n'))
                else:
                    dict.get(list[0])[list[1]] = [list[2].strip('\n')]
            else:
                dict[list[0]] = {list[1]:[list[2].strip('\n')]}

        # for key in dict.keys():
        #     print(key+' : ',dict[k])
        file.close()

    return dict


def DFS(dict, dg, node, depth=3):
    depth -= 1

    if depth < 0:
        return dg
    if node not in dict.keys():
        return dg
    sequence = dict[node]
    count = 0
    for key in sequence.keys():
        if not dg.has_node(key):
            dg.add_node(key)
        if not dg.has_edge((node, key)):
            dg.add_edge((node, key), wt=len(sequence[key]))
            count += len(sequence[key])
        else:
            continue
            # print(node, key, dg.edge_weight((node, key)), len(sequence[key]))

        # array[int(node)][int(key)] = len(sequence[key])
        dg = DFS(dict, dg, key, depth)

    for n in dg.neighbors(node):
        dg.set_edge_weight((node, n),wt= float(dg.edge_weight((node, n))/max(count,1)))

    return dg

def compute_dfs(lines, file_subGraphs, dict_):
    pid, lines = lines
    if pid == 0:
        lines = tqdm(lines)
    for line in lines:
        list = line.split("	")
        node0 = list[1].strip('\n')
        #print("node0-----", node0)

        dg = digraph()
        dg.add_node(node0)
        #t1 = time.perf_counter()
        dg = DFS(dict_, dg, node0, depth=4)

        fo = open(file_subGraphs + node0 + ".txt", "w")
        NODE = ""
        for nodei in dg.nodes():
            NODE = NODE +nodei+ "\t"
        fo.write(NODE+'\n')

        for e in dg.edges():
            fo.write(e[0] + "\t" + e[1] + "\t" + str(dg.edge_weight(e))+'\n')
        fo.close()



        #t2=time.perf_counter()
        # time.sleep(1)
        #print(t2-t1)
        # print(dg.nodes().__len__())
        # for edge in dg.edges():
        #     print('edge----',edge)



def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--file-data", required=True)
    parser.add_argument("-t", "--threads", type=int, default=1)
    args = parser.parse_args()
    #file_data = "../data/TCdata/"
    #file_entity = file_data + "/FB15K/entity2id.txt"
    file_entity = os.path.join(args.file_data, "entity2id.txt")
    #file_train = file_data + "/FB15K/golddataset/train2id.txt"
    file_train = os.path.join(args.file_data, "train2id.txt")
    #file_test = file_data + "/FB15K/golddataset/test2id.txt"
    file_test = os.path.join(args.file_data, "test2id.txt")
    #file_valid = file_data + "/FB15K/golddataset/valid2id.txt"
    file_valid = os.path.join(args.file_data, "valid2id.txt")
    #file_subGraphs = file_data + "/subGraphs_4_3/"
    file_subGraphs = os.path.join(args.file_data, "subGraphs_4/")

    if not os.path.exists(file_subGraphs):
        os.mkdir(file_subGraphs)

    #dict_ = ReadAllTriples([file_train, file_test, file_valid])
    #dict_ = ReadAllTriples([file_train, file_valid])
    dict_ = ReadAllTriples([file_train])
    print("dict size--", len(dict_))
    print("ReadAllTriples is done!")

    file = open(file_entity, "r")
    lines = file.readlines()
    file.close()

    with concurrent.futures.ProcessPoolExecutor(max_workers=args.threads) as executor:
        write_futures = [executor.submit(compute_dfs,
                                         chunk,
                                         file_subGraphs=file_subGraphs,
                                         dict_=dict_)
                         for chunk in enumerate(chunked(lines, len(lines)//args.threads))
                         ]
        for future in concurrent.futures.as_completed(write_futures):
            res = future.result()


    # files = os.listdir(file_subGraphs)
    # for f in files:
    #     print(f)
    #     fo = open(os.path.join(file_subGraphs, f),'r')
    #     fin = open(os.path.join(file_data + "/subGraphs_444/", f),'w')
    #     lines = fo.readlines()
    #     for i, line in enumerate(lines):
    #         nodes = line.rstrip('\t\n').rstrip('\t').rstrip('\n').split('\t')
    #
    #         if i == 0:
    #             for node in nodes:
    #                 if node == '0':
    #                     node = '14951'
    #                 fin.write(node + '\t')
    #             fin.write('\n')
    #         else:
    #             if nodes[0] == '0':
    #                 nodes[0] = '14951'
    #             if nodes[1] == '0':
    #                 nodes[1] = '14951'
    #             fin.write(nodes[0] + '\t' + nodes[1] + '\t' + nodes[2] + '\n')
    #
    #     fin.close()
    #     fo.close()








if __name__ == '__main__':
    main()
