from greplin import scales
import time
import threading
import os

XTANDEM = './tandem'
class Job4(threading.Thread):
    state = scales.Stat('state')
    finished = scales.IntStat('finished')

    def __init__(self, name, s3conn, bucket_name, key_names, input_xml):
        threading.Thread.__init__(self)  
        scales.init(self, name)
        self.s3conn = s3conn
        self.bucket_name = bucket_name
        self.key_names = key_names
        self.input_xml = input_xml

    def run(self):
        self.state = 'accessing bucket ' + self.bucket_name
        bucket = self.s3conn.get_bucket(self.bucket_name)
        for key_name in self.key_names:
            self.state = 'downloading ' + key_name
            key = bucket.get_key(key_name)
            try:
                key.get_contents_to_filename(key_name)
            except Exception, ex:
                self.state = 'error happened when downloading ' + key_name
                print Exception, ':', ex
                return

        cmd = XTANDEM + ' ' + self.input_xml
        self.state = 'xtandem searching'
        os.system(cmd)
        self.state = 'done'
        self.finished = 1
