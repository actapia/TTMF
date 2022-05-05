from TransConfidence import *
import PrecessData

try:
    from IPython import embed
except ImportError:
    pass

if __name__ == "__main__":

    file_data = "./data"

    entity2idfile = file_data + "/FB15K/entity2id.txt"
    relation2idfile = file_data + "/FB15K/relation2id.txt"
    entity2vecfile =file_data + "/FB15K_TransE_Entity2Vec_100.txt"
    relation2vecfile = file_data + "/FB15K_TransE_Relation2Vec_100.txt"
    # entity2vecfile =file_data + "/entity2vec_100.txt"
    # relation2vecfile = file_data + "/relation2vec_100.txt"

    # trainfile = file_data + "/KBE/datasets/FB15k/train2id.txt"
    devfile = file_data + "/KBE/datasets/FB15k/conf_train2id.txt"
    testfile = file_data + "/KBE/datasets/FB15k/conf_test2id.txt"
    # testfile_KGC__rt = file_data + "/FB15K/KBCdataset/_rt.txt"
    # testfile = testfile_KGC__rt

    # train_transE_file = file_data + "/KBE/datasets/FB15k/valid_TransE_confidence.txt"
    # dev_transE_file = file_data + "/KBE/datasets/FB15k/test_TransE_confidence.txt"
    # test_transE_file = file_data + "/KBE/datasets/FB15k/test_TransE_confidence.txt"


    print('start...')

    ent_vocab, ent_idex_word = PrecessData.get_index(entity2idfile)
    rel_vocab, rel_idex_word = PrecessData.get_index(relation2idfile)
    print("entity vocab size: ", str(len(ent_vocab)), str(len(ent_idex_word)))
    print("relation vocab size: ", str(len(rel_vocab)), str(len(rel_idex_word)))

    entvec_k, entity2vec = PrecessData.load_vec_txt(entity2vecfile, ent_vocab, k=100)
    print("word2vec loaded!")
    print("entity2vec  size: " + str(len(entity2vec)))

    relvec_k, relation2vec = PrecessData.load_vec_txt(relation2vecfile, rel_vocab, k=100)
    print("word2vec loaded!")
    print("relation2vec  size: " + str(len(relation2vec)))

    # print('trainfile-----')
    # tcTrainExamples, confidence = get_data_txt(testfile)
    tcDevExamples, confidence = PrecessData.get_data_txt(devfile)


    # get_TransConfidence(threshold_dict, tcTrainExamples, entity2vec, relation2vec)

    print('devfile-----')
    threshold_dict = tcThreshold(tcDevExamples, entity2vec, relation2vec)

    cdic_dev = get_TransConfidence(threshold_dict, tcDevExamples, entity2vec, relation2vec)

    print('testfile-----')
    tcTestExamples, confidence = PrecessData.get_data_txt(testfile)
    cdic_test= get_TransConfidence(threshold_dict, tcTestExamples, entity2vec, relation2vec)

    #embed()

