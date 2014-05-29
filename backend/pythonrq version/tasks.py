import requests
import os
from boto.s3.connection import S3Connection
from greplin import scales

XTANDEM = './tandem'

class Handler(object):
	requests = scales.IntStat('requests')

	def __init__(self):
		scales.init(self, '/handler')

	def handleRequest(self):
		self.requests += 1

def count_words_at_url(url, msgQueue):
	msg = msgQueue.get()
	print id(msg)
	resp = requests.get(url)
	return len(resp.text.split())

def job4(access_key, secret_key, bucket_name, key_names, input_xml):
	s3conn = S3Connection(access_key, secret_key)
	bucket = s3conn.get_bucket(bucket_name)
	print '+++++++++++++++'

	for key_name in key_names:
		print key_name
		key = bucket.get_key(key_name)
		key.get_contents_to_filename(key_name)

	cmd = XTANDEM + ' ' + input_xml
	os.system(cmd)
	return 'done'