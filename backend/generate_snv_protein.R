library(customProDB)
vcffile <- "/root/Bio/databases/dbsnp/All.vcf"
vcf <- InputVcf(vcffile)
table(values(vcf[[1]])[['INDEL']])
index <- which(values(vcf[[1]])[['INDEL']] == FALSE)
SNVvcf <- vcf[[1]][index]
load("/root/annotation_db/exon_anno.RData")
load("/root/annotation_db/dbsnpinCoding.RData")
load("/root/annotation_db/procodingseq.RData")
load("/root/annotation_db/ids.RData")
load("/root/annotation_db/proseq.RData")
postable_snv <- Positionincoding(SNVvcf, exon, dbsnpinCoding)
txlist <- unique(postable_snv[, 'txid'])
codingseq <- procodingseq[procodingseq[, 'tx_id'] %in% txlist, ]
mtab <- aaVariation (postable_snv, codingseq)
outfile <- "/root/all_snv_single.fasta"
OutputVarproseq_single(mtab, proteinseq, outfile, ids, lablersid=TRUE)



library(sapFinder)
vcf <- "/root/Bio/databases/dbsnp/All.vcf"
annotation <- "/root/sap_finder/hg19_refGeneSapFinder.txt"
refseq <- "/root/sap_finder/hg19_refGeneMrnaSapFinder.fa"
outdir <- "sapfinder_output"
prefix <- "dbsnp_all"
db.files <- dbCreator(vcf=vcf, annotation=annotation, refseq=refseq, outdir=outdir, prefix=prefix)

