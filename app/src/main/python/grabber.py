import gallery_dl
import json

def extract_info(url):
    results = []
    class InfoJob(gallery_dl.job.DataJob):
        def handle_url(self, url, kwdict):
            results.append({
                "url": url,
                "title": str(kwdict.get("title") or kwdict.get("description") or kwdict.get("filename") or ""),
                "uploader": str(kwdict.get("author", {}).get("name") if isinstance(kwdict.get("author"), dict) else (kwdict.get("user") or kwdict.get("uploader") or "")),
                "ext": str(kwdict.get("extension") or "jpg"),
                "id": str(kwdict.get("id") or kwdict.get("post_id") or len(results))
            })
        def handle_directory(self, kwdict):
            pass

    job = InfoJob(url)
    job.run()
    return json.dumps(results)

def download_gallery(url, output_dir):
    gallery_dl.config.set(("extractor",), "base-directory", output_dir)
    gallery_dl.config.set(("extractor",), "directory", [])
    job = gallery_dl.job.DownloadJob(url)
    return job.run()
