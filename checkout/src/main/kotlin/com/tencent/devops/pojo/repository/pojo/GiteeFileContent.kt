package com.tencent.devops.repository.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GiteeFileContent(
    var type: String,
    var encoding: String,
    var size: Long,
    var name: String,
    var path: String,
    var content: String,
    var sha: String,
    var url: String,
    @JsonProperty("html_url")
    var htmlUrl: String,
    @JsonProperty("download_url")
    var downloadUrl: String,
    @JsonProperty("_links")
    var links: Links
) {
    data class Links(
        var self: String,
        var html: String
    )
}
