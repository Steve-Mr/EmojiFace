import json
import os
import requests

apiAddress = "http://127.0.0.1:8081/"
urlPrefix = apiAddress + "bot" + os.getenv("TELEGRAM_TOKEN")


def findString(sourceStr, targetStr):
    if str(sourceStr).find(str(targetStr)) == -1:
        return False
    else:
        return True


def genFileDirectory(path):
    files_walk = os.walk(path)
    target = {
    }
    for root, dirs, file_name_dic in files_walk:
        for fileName in file_name_dic:
            if findString(fileName, "v8a"):
                target["arm64"] = (fileName, open(path + "/" + fileName, "rb"))
            if findString(fileName, "v7a"):
                target["armeabi"] = (fileName, open(path + "/" + fileName, "rb"))
            if findString(fileName, "x86.apk"):
                target["i386"] = (fileName, open(path + "/" + fileName, "rb"))
            if findString(fileName, "x86_64"):
                target["amd64"] = (fileName, open(path + "/" + fileName, "rb"))

    return target


def sendDocument(user_id, path, message = "", entities = None):
    files = {'document': open(path, 'rb')}
    data = {'chat_id': user_id,
            'caption': message,
            'parse_mode': 'Markdown',
            'caption_entities': entities}
    response = requests.post(urlPrefix + "/sendDocument", files=files, data=data)
    print(response.json())


def sendAPKs(path):
    apks = os.listdir("apks")
    apks.sort()
    apk = os.path.join("apks", apks[0])
    sendDocument(user_id="@maaryIsTyping", path = apk, message="#app #apk #FaceMoji https://github.com/Steve-Mr/EmojiFace")

if __name__ == '__main__':
    sendAPKs("./apks")

