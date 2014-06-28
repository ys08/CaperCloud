import sys
import boto
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.key import Key

s3_host = "192.168.99.111"
access_key = sys.argv[1]
secret_key = sys.argv[2]
 
# Setup connection to Walrus
s3conn = boto.connect_s3(aws_access_key_id=access_key,
                       aws_secret_access_key=secret_key,
                       is_secure=False,
                       host=s3_host,
                       port=8773,
                       path="/services/Walrus",
                       calling_format=OrdinaryCallingFormat())
 
# Run commands
buckets = s3conn.get_all_buckets()
print buckets
b = s3conn.get_bucket("capercloud-ref")
print b.list

