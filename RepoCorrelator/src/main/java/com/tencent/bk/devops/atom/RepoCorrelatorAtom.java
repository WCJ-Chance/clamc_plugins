package com.tencent.bk.devops.atom;

import com.tencent.bk.devops.atom.pojo.AtomParam;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.pojo.DataField;
import com.tencent.bk.devops.atom.service.GitlabApiService;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AtomService(paramClass = AtomParam.class)
public class RepoCorrelatorAtom implements TaskAtom<AtomParam> {

    private static final Logger logger = LoggerFactory.getLogger(RepoCorrelatorAtom.class);

    @SneakyThrows
    @Override
    public void execute(AtomContext<AtomParam> atomContext) {
        AtomParam param = atomContext.getParam();
        AtomResult result = atomContext.getResult();
        if (result.getStatus() != Status.success) {
            return;
        }

        Map<String, String> typeToUrlList = new HashMap<>();
        GitlabApiService gitlabApiService = new GitlabApiService(
                atomContext.getSensitiveConfParam("targetGitlabUrl"),
                atomContext.getSensitiveConfParam("targetGitlabAccessToken"),
                param.getProjectCnName(),
                param.getProjectEnName()
        );
        if (gitlabApiService.getParentId() == null || gitlabApiService.getBackendId() == null || gitlabApiService.getUiId() == null || gitlabApiService.getTestId() == null) {
            result.setStatus(Status.failure);// 状态设置为失败
            result.setMessage("群组关联创建失败");
            return;
        }
        typeToUrlList.put(gitlabApiService.getBackendId(), param.getBackend());
        typeToUrlList.put(gitlabApiService.getUiId(), param.getUi());
        typeToUrlList.put(gitlabApiService.getTestId(), param.getTest());

        UsernamePasswordCredentialsProvider sourceGitlabAccessToken = new UsernamePasswordCredentialsProvider(
                "PRIVATE-TOKEN",
                atomContext.getSensitiveConfParam("sourceGitlabAccessToken")
        );
        UsernamePasswordCredentialsProvider targetGitlabAccessToken = new UsernamePasswordCredentialsProvider(
                "PRIVATE-TOKEN",
                atomContext.getSensitiveConfParam("targetGitlabAccessToken")
        );
        File workSpace = new File(param.getBkWorkspace());
        String pattern = "http://.*/(.*)\\.git";
        Pattern p = Pattern.compile(pattern);

        String pattern1 = "refs/heads/(.*)";
        Pattern p1 = Pattern.compile(pattern1);

        for (Map.Entry<String, String> entry : typeToUrlList.entrySet()
             ) {
            Matcher m = p.matcher(entry.getValue());
            while (m.find()) {
                String sourceAddress = m.group(0);
                String projectName = m.group(1);
                String targetAddress = gitlabApiService.projectIsExsit(projectName);
                if (targetAddress == null) { // 需要目标创建代码库
                    targetAddress = gitlabApiService.createProject(projectName, entry.getKey());
                }
                clearWorkspace(workSpace);
                Git git = Git.cloneRepository()
                        .setURI(sourceAddress)
                        .setDirectory(workSpace)
                        .setCredentialsProvider(sourceGitlabAccessToken)
                        .call();
                Collection<Ref> refs = git.lsRemote()
                        .setCredentialsProvider(sourceGitlabAccessToken)
                        .setRemote("origin")
                        .setHeads(true)
                        .setTags(true)
                        .call();
                for (Ref ref : refs
                     ) {
                    String remoteRefName = ref.getName();
                    Matcher m1 = p1.matcher(remoteRefName);
                    if (m1.find() && git.getRepository().getRef(remoteRefName) == null) { //本地没有的ref就去拉取
                        git.checkout()
                                .setCreateBranch(true)
                                .setName(m1.group(1))
                                .setStartPoint("origin/" + m1.group(1))
                                .call();
                    }
                }
                git.push()
                        .setCredentialsProvider(targetGitlabAccessToken)
                        .setRemote(targetAddress)
                        .setPushAll()
                        .setPushTags()
                        .setForce(true)
                        .call();
                logger.info("代码库迁移{}--->{}完毕，请确认!", sourceAddress, targetAddress);
            }
            logger.info("----------------------------------");
        }

    }


    public static void clearWorkspace(File file) {
        if (file.isFile() || file.list().length == 0) {
            file.delete();
        } else {
            for (File f : file.listFiles()
            ) {
                clearWorkspace(f);
            }
            file.delete();
        }
    }

    @SneakyThrows
    public static void main(String[] args) {
//        GitLabApi gitLabApi = new GitLabApi("https://code.canway.net/", "tVormjXizKrezXM9Ctz4");
        File workspace = new File("C:\\Users\\chancew\\Desktop\\JGitTest");
        Git git = Git.cloneRepository()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("PRIVATE-TOKEN", "Zoysc-JQYPsnK1uDhPxk"))
                .setURI("https://code.canway.net/soft-fy21-clamc-devops/pipeline-plugin.git")
                .setCloneAllBranches(true)
                .setDirectory(workspace)
                .call();
//        Git git = Git.open(workspace);
        Collection<Ref> refs = git.lsRemote()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("PRIVATE-TOKEN", "Zoysc-JQYPsnK1uDhPxk"))
                .setRemote("origin")
                .setHeads(true)
                .setTags(true)
                .call();
        String pattern = "refs/heads/(.*)";
        Pattern p = Pattern.compile(pattern);

        for (Ref ref : refs
             ) {
            String remote = ref.getName();
            logger.info(remote);
            Matcher m = p.matcher(remote);
            if (m.find() && !m.group(1).equals("master")) {
                logger.info(m.group(1));
                git.checkout()
                        .setCreateBranch(true)
                        .setName(m.group(1))
                        .setStartPoint("origin/" + m.group(1))
                        .call();
            }
        }
    }

}
