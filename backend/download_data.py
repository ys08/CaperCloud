#!/usr/bin/python

import sys
import boto

euca_ec2_host="192.168.99.111"
euca_s3_host="192.168.99.111"

euca_id = sys.argv[0]
euca_key = sys.argv[1]

s3conn = boto.connect_s3(
    aws_access_key_id=euca_id,
    aws_secret_access_key=euca_key,
    is_secure=False,
    port=8773,
    path="/services/Walrus",
    host=euca_s3_host)

s3conn.get_all_buckets()
