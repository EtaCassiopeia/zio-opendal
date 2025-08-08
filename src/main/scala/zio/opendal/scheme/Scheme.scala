package zio.opendal.scheme

sealed trait Scheme {
  def name: String
}

/** Cloud storage schemes for major cloud providers */
object CloudStorage {
  case object S3     extends Scheme { def name = "s3"     }
  case object Azblob extends Scheme { def name = "azblob" }
  case object Azdls  extends Scheme { def name = "azdls"  }
  case object Azfile extends Scheme { def name = "azfile" }
  case object Gcs    extends Scheme { def name = "gcs"    }
  case object Cos    extends Scheme { def name = "cos"    }
  case object Oss    extends Scheme { def name = "oss"    }
  case object Obs    extends Scheme { def name = "obs"    }
  case object Swift  extends Scheme { def name = "swift"  }
  case object B2     extends Scheme { def name = "b2"     }
}

/** Local storage schemes including filesystem and in-memory */
object LocalStorage {
  case object Fs       extends Scheme { def name = "fs"        }
  case object Memory   extends Scheme { def name = "memory"    }
  case object Dashmap  extends Scheme { def name = "dashmap"   }
  case object MiniMoka extends Scheme { def name = "mini_moka" }
  case object Moka     extends Scheme { def name = "moka"      }
}

/** Database storage schemes */
object Database {
  case object Mysql        extends Scheme { def name = "mysql"        }
  case object Postgresql   extends Scheme { def name = "postgresql"   }
  case object Sqlite       extends Scheme { def name = "sqlite"       }
  case object Mongodb      extends Scheme { def name = "mongodb"      }
  case object Redis        extends Scheme { def name = "redis"        }
  case object Memcached    extends Scheme { def name = "memcached"    }
  case object Etcd         extends Scheme { def name = "etcd"         }
  case object Rocksdb      extends Scheme { def name = "rocksdb"      }
  case object Redb         extends Scheme { def name = "redb"         }
  case object Sled         extends Scheme { def name = "sled"         }
  case object Foundationdb extends Scheme { def name = "foundationdb" }
  case object Tikv         extends Scheme { def name = "tikv"         }
  case object Surrealdb    extends Scheme { def name = "surrealdb"    }
  case object Persy        extends Scheme { def name = "persy"        }
}

/** Specialized storage schemes for specific services */
object Specialized {
  // Developer platforms
  case object Github          extends Scheme { def name = "github"           }
  case object Ghac            extends Scheme { def name = "ghac"             }
  case object VercelArtifacts extends Scheme { def name = "vercel_artifacts" }
  case object VercelBlob      extends Scheme { def name = "vercel_blob"      }
  case object Huggingface     extends Scheme { def name = "huggingface"      }

  // Content delivery and edge
  case object CloudflareKv extends Scheme { def name = "cloudflare_kv" }
  case object D1           extends Scheme { def name = "d1"            }
  case object Cacache      extends Scheme { def name = "cacache"       }

  // Personal cloud drives
  case object Dropbox     extends Scheme { def name = "dropbox"      }
  case object Onedrive    extends Scheme { def name = "onedrive"     }
  case object Gdrive      extends Scheme { def name = "gdrive"       }
  case object YandexDisk  extends Scheme { def name = "yandex_disk"  }
  case object AliyunDrive extends Scheme { def name = "aliyun_drive" }
  case object Pcloud      extends Scheme { def name = "pcloud"       }
  case object Koofr       extends Scheme { def name = "koofr"        }

  // Network protocols
  case object Http   extends Scheme { def name = "http"   }
  case object Ftp    extends Scheme { def name = "ftp"    }
  case object Sftp   extends Scheme { def name = "sftp"   }
  case object Webdav extends Scheme { def name = "webdav" }

  // Distributed filesystems
  case object Hdfs       extends Scheme { def name = "hdfs"        }
  case object HdfsNative extends Scheme { def name = "hdfs_native" }
  case object Webhdfs    extends Scheme { def name = "webhdfs"     }
  case object Alluxio    extends Scheme { def name = "alluxio"     }
  case object Monoiofs   extends Scheme { def name = "monoiofs"    }

  // Analytics and data platforms
  case object Dbfs    extends Scheme { def name = "dbfs"    }
  case object Lakefs  extends Scheme { def name = "lakefs"  }
  case object Seafile extends Scheme { def name = "seafile" }

  // Other specialized services
  case object Ipfs   extends Scheme { def name = "ipfs"   }
  case object Ipmfs  extends Scheme { def name = "ipmfs"  }
  case object Gridfs extends Scheme { def name = "gridfs" }
  case object Compfs extends Scheme { def name = "compfs" }
  case object Upyun  extends Scheme { def name = "upyun"  }
}
