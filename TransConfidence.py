# -*- coding: utf-8 -*-


import math
import numpy as np
#import PrecessData

def getThreshold(rrank):

    distanceFlagList = rrank
    distanceFlagList = sorted(distanceFlagList, key=lambda sp: sp[0], reverse=False)

    threshold = distanceFlagList[0][0] - 0.01
    maxValue = 0
    currentValue = 0
    for i in range(1, len(distanceFlagList)):
        if distanceFlagList[i - 1][1] == 1:
            currentValue += 1
        else:
            currentValue -= 1

        if currentValue > maxValue:
            threshold = (distanceFlagList[i][0] + distanceFlagList[i - 1][0]) / 2.0
            maxValue = currentValue
    # print('threshold... ', threshold)
    return threshold



def tcThreshold(tcDevExamples, entity2vec, relation2vec):

    threshold_dict = {}
    trans_dict = {}

    for tri in tcDevExamples:

        s = entity2vec[tri[0]] + relation2vec[tri[2]] - entity2vec[tri[1]]
        transV = np.linalg.norm(s, ord=2)

        if tri[2] not in trans_dict.keys():
            trans_dict[tri[2]] = [(transV, tri[3])]
        else:
            trans_dict[tri[2]].append((transV, tri[3]))

    # for tri in tcDevExamples[1]:
    #
    #     s = entity2vec[tri[0]] + relation2vec[tri[2]] - entity2vec[tri[1]]
    #     transV = np.linalg.norm(s, ord=2)
    #
    #     if tri[2] not in trans_dict.keys():
    #         trans_dict[tri[2]] = [(transV, tri[3])]
    #         for tri2 in tcDevExamples[1]:
    #             if tri[2] == tri2[2]:
    #                 s = entity2vec[tri2[0]] + relation2vec[tri2[2]] - entity2vec[tri2[1]]
    #                 transV = np.linalg.norm(s, ord=2)
    #                 trans_dict[tri2[2]].append((transV, tri2[3]))

    for it in trans_dict.keys():
        threshold_dict[it] = getThreshold(trans_dict[it])


    return threshold_dict


def get_TransConfidence(threshold_dict, tcExamples, entity2vec, relation2vec):

    All_conf = 0.0
    confidence_dict = []

    right = 0.0
    for triple in tcExamples:
        if triple[2] in threshold_dict.keys():
            threshold = threshold_dict[triple[2]]
        else:
            # print('threshold is None !!!!!!!!!')
            threshold = 0.0

        s = entity2vec[triple[0]] + relation2vec[triple[2]] - entity2vec[triple[1]]
        transV = np.linalg.norm(s, ord=2)
        f = 1.0 / (1.0 + math.exp(-1 * (threshold - transV)))
        f = (threshold - transV)
        # print('threshold= ', threshold, 'rankvalue= ', transV, 'flag= ', triple[3], 'f= ', f)

        confidence_dict.append(f)

        if transV <= threshold and triple[3] == 1:
            right += 1.0
            All_conf += f

        elif transV > threshold and triple[3] == -1:
            right += 1.0


    print('TransConfidence accuracy ---- ', right / len(tcExamples))

    avg_conf = All_conf / float(len(tcExamples))
    print('avg_confidence ... ', avg_conf, float(len(tcExamples)))

    return confidence_dict


