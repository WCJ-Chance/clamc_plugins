package com.tencent.bk.devops.atom.task

import com.tencent.bk.devops.atom.pojo.AtomBaseParam
import lombok.Data
import lombok.EqualsAndHashCode


@Data
@EqualsAndHashCode(callSuper = true)
class DockerAtomParam : AtomBaseParam() {

    val sourceMirrorTicketPair: String? = null
    val targetImage: String = ""
    val targetImageTag: String = ""
    val targetTicketId: String? = null
    val dockerBuildDir: String? = null
    val dockerFilePath: String? = null
    var dockerBuildArgs: String? = null
    val enableProxy: Boolean? = false
    val dockerBuildHosts: String? = null

}
