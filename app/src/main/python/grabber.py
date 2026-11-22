import gallery_dl
import json

def download_gallery(url, output_dir):
    # Mengatur folder penyimpanan hasil download
    gallery_dl.config.set(("extractor",), "base-directory", output_dir)
    gallery_dl.config.set(("extractor",), "directory", [])
    
    # Menjalankan proses download gallery-dl
    job = gallery_dl.job.DownloadJob(url)
    status = job.run()
    return status # 0 berarti sukses
