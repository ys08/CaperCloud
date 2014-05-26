#!/usr/local/bin/python
import os

from flask import Flask
from flask import request
from boto.s3.connection import S3Connection
from werkzeug.utils import secure_filename

# from greplin import scales
# import greplin.scales.flaskhandler as statserver

# STATS = scales.collection('/web',
# 	scales.IntStat('errors'),
# 	scales.IntStat('success'))
# statserver.serveInBackground(8765, serverName='capercloud-status-server')
UPLOAD_FOLDER = './'
XTANDEM = './tandem'

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024

@app.route('/login', methods=['POST'])
def login():
	if request.method == 'POST':
		# global access_key
		# global secret_key
		access_key = request.form['access_key']
		secret_key = request.form['secret_key']
		return 'login success'

@app.route('/s3/<operation>', methods=['POST'])
def s3(operation):
	if request.method == 'POST':
		if operation == 'download':
			bucket_name = request.form['bucket_name']
			key_name = request.form['key_name']
			s3conn = S3Connection(access_key, secret_key)
			bucket = s3conn.get_bucket(bucket_name)
			key = bucket.get_key(key_name)
			key.get_contents_to_filename(key_name)
			return 'download success'

		if operation == 'multidownload':
			bucket_name = request.form['bucket_name']
			key_names = request.form['key_names'].split(',')
			s3conn = S3Connection(access_key, secret_key)
			bucket = s3conn.get_bucket(bucket_name)

			for key_name in key_names:
				key = bucket.get_key(key_name)
				key.get_contents_to_filename(key_name)
			return 'multidownload success'

		if operation == 'upload':
			return 'uploading to do'

		if operation == 'list':
			bucket_name = request.form['bucket_name']
			s3conn = S3Connection(access_key, secret_key)
			bucket = s3conn.get_bucket(bucket_name)
			res = []
			for key in bucket.list():
				res.append(key.name)
			return '\n'.join(res)

@app.route('/ec2/', methods=['POST'])
def ec2():
	if request.method == 'POST':
		pass

@app.route('/xtandem', methods=['POST'])
def xtandem():
	if request.method == 'POST':
		input_file = request.form['input_file']
		cmd = xtandem + ' ' + input_file
		os.system(cmd)
		return 'xtandem search success'

@app.route('/filter', methods=['POST'])
def filter():
	if request.method == 'POST':
		txml_file = request.form['txml_file']
		#TO DO
		return 'filter success'

@app.route('/result', methods=['POST'])
def result():
	if request.method == 'POST':
		#TO DO
		pass

@app.route('/job4', methods=['POST'])
def job4():
	if request.method == 'POST':
		input_xml = request.form['input_xml']
		access_key = request.form['access_key']
		secret_key = request.form['secret_key']
		bucket_name = request.form['bucket_name']
		key_names = request.form['key_names'].split(',')

		s3conn = S3Connection(access_key, secret_key)
		print "+++++"
		bucket = s3conn.get_bucket(bucket_name)
		print "-----"

		for key_name in key_names:
			key = bucket.get_key(key_name)
			print "downloading " + key_name
			key.get_contents_to_filename(key_name)

		print "run xtandem"
		cmd = XTANDEM + ' ' + input_xml
		os.system(cmd)
		return 'success'

@app.route('/', methods=['POST'])
def upload_file():
	if request.method == 'POST':
		# parameter file		
		for file in request.files.itervalues():
			if file:
				filename = secure_filename(file.name)
				print filename
				file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
				print os.path.join(app.config['UPLOAD_FOLDER'], filename)
		return 'success'

if __name__ == '__main__':
	app.run(debug=True)


