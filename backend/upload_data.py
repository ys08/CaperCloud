import sys
import os
import boto
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.key import Key

s3_host = "192.168.99.111"
access_key = sys.argv[1]
secret_key = sys.argv[2]
bucket_name = sys.argv[3]
full_path = sys.argv[4]
file_name = os.path.basename(full_path)
 
# Setup connection to Walrus
s3conn = boto.connect_s3(aws_access_key_id=access_key,
                       aws_secret_access_key=secret_key,
                       is_secure=False,
                       host=s3_host,
                       port=8773,
                       path="/services/Walrus",
                       calling_format=OrdinaryCallingFormat())
 
# Run commands
print "uploading result to bucket " + bucket_name

try:
    b = s3conn.get_bucket(bucket_name)
except boto.exception.S3ResponseError:
    b = s3conn.create_bucket(bucket_name)
k = Key(b)
k.key = file_name
k.set_contents_from_filename(full_path)

