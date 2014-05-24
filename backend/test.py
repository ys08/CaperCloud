#!/usr/local/bin/python

import requests

login_data = {'access_key': 'AKIAIFCPNVFB6YNZYWXQ', 'secret_key': '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'}
r1 = requests.post("http://127.0.0.1:5000/login", data=login_data)
print r1.text

s3_multidownload_data = {'bucket_name': 'bprc-data', 'key_name': '1400835843276taxonomy.xml,qExactive01819.mgf1400835843276.xml,qExactive01819.mgf,uniprot-human-reviewed-march-2014_concatenated_target_decoy.fasta'}
requests.post("http://127.0.0.1:5000/s3/multidownload", data=s3_download_taxonomy_data)

# s3_list_data = {'bucket_name': 'bprc-data'}
# r3 = requests.post("http://127.0.0.1:5000/s3/list", data=s3_list_data)
# print r3.text

xtandem_data = {'input_file': 'qExactive01819.mgf1400835843276.xml'}

