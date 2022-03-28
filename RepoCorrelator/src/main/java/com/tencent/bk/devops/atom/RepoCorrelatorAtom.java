package com.tencent.bk.devops.atom;

import com.tencent.bk.devops.atom.pojo.AtomParam;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupParams;
import org.gitlab4j.api.models.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
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

        GitLabApi sourceGitLabApi = new GitLabApi(atomContext.getSensitiveConfParam("sourceGitLabUrl"), atomContext.getSensitiveConfParam("sourceGitLabAccessToken"));
        GitLabApi targetGitLabApi = new GitLabApi(atomContext.getSensitiveConfParam("targetGitLabUrl"), atomContext.getSensitiveConfParam("targetGitLabAccessToken"));
        Map<String, String> sourceToTargetHttpUrlMap = new HashMap<String, String>();

        UsernamePasswordCredentialsProvider sourceGitlabAccessToken = new UsernamePasswordCredentialsProvider(
                "PRIVATE-TOKEN",
                atomContext.getSensitiveConfParam("sourceGitLabAccessToken")
        );
        UsernamePasswordCredentialsProvider targetGitlabAccessToken = new UsernamePasswordCredentialsProvider(
                "PRIVATE-TOKEN",
                atomContext.getSensitiveConfParam("targetGitLabAccessToken")
        );

//        File workSpace = new File(param.getBkWorkspace());
        String workSpace = param.getBkWorkspace();
        if (param.getFeatureOption().equals(0)) {
            String pattern = atomContext.getSensitiveConfParam("sourceGitLabUrl") + "/(.*)";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(param.getSourceGroupUrl());
            Group sourceRootGroup;
            if (m.find()) {
                sourceRootGroup = sourceGitLabApi.getGroupApi().getGroup(m.group(1));
            } else {
                logger.error("没有匹配到源分组路径");
                result.setStatus(Status.failure);// 状态设置为失败
                result.setMessage("没有匹配到源分组路径");
                return;
            }
            dfs(param.getRootId(), sourceGitLabApi, targetGitLabApi, sourceRootGroup, sourceToTargetHttpUrlMap, result);
            pullAndPush(workSpace, sourceToTargetHttpUrlMap, sourceGitlabAccessToken, targetGitlabAccessToken);
        }
        else if (param.getFeatureOption().equals(1)) {
            String pattern = "http://.*\\.git";
            String pattern1 = "http://git.clamc.com/(.*/(.*))\\.git";
            Pattern p = Pattern.compile(pattern);
            Pattern p1 = Pattern.compile(pattern1);
            Matcher m = p.matcher(param.getRepoUrlList());
            while (m.find()) {
                Matcher m1 = p1.matcher(m.group());
                if (m1.find()) {
                    Project sp = sourceGitLabApi.getProjectApi().getProject(m1.group(1));
                    Project tp = new Project().withName(sp.getName())
                            .withDescription(sp.getDescription())
                            .withPath(sp.getPath().toLowerCase(Locale.ROOT))
                            .withNamespaceId(Integer.parseInt(param.getRootId()));
                    Project targetProject;
                    try {
                        targetProject = targetGitLabApi.getProjectApi().createProject(tp);
                    } catch (GitLabApiException e) {
                        logger.error("目标项目已存在");
                        Group targetRootGroup = targetGitLabApi.getGroupApi().getGroup(Integer.parseInt(param.getRootId()));
                        targetProject = targetGitLabApi.getProjectApi().getProject(targetRootGroup.getFullPath() + m1.group(2));
                    }
                    sourceToTargetHttpUrlMap.put(sp.getHttpUrlToRepo(), targetProject.getHttpUrlToRepo());
                }
            }
            pullAndPush(workSpace, sourceToTargetHttpUrlMap, sourceGitlabAccessToken, targetGitlabAccessToken);
            logger.info("项目更新成功！");
            result.setStatus(Status.success);
            result.setMessage("项目同步成功！");
        }
        else if (param.getFeatureOption().equals(2)) {
            if (param.isIfSetAllConfig()) {
                Group climbConfig = sourceGitLabApi.getGroupApi().getGroup("ClimbConfig");
                dfs("0", sourceGitLabApi, targetGitLabApi, climbConfig, sourceToTargetHttpUrlMap, result);
                if (sourceToTargetHttpUrlMap.containsKey("http://git.clamc.com/Climb/Base/Report.git")) {
                    sourceToTargetHttpUrlMap.remove("http://git.clamc.com/Climb/Base/Report.git");
                }
                pullAndPush(workSpace, sourceToTargetHttpUrlMap, sourceGitlabAccessToken, targetGitlabAccessToken);
                logger.info("配置文件代码库全量迁移成功!");
                result.setStatus(Status.success);
                result.setMessage("配置文件代码库全量迁移成功!");
                return;
            }
            String pattern = "http://.*\\.git";
            String pattern1 = "http://git.clamc.com/.*/(.*)\\.git";
            Pattern p = Pattern.compile(pattern);
            Pattern p1 = Pattern.compile(pattern1);
            Matcher m = p.matcher(param.getConfigUrlList());
            while (m.find()) {
                Matcher m1 = p1.matcher(m.group());
                if (m1.find()) {
                    sourceToTargetHttpUrlMap.put(m.group(), atomContext.getSensitiveConfParam("targetGitLabUrl") + "/climbconfig/" + m1.group(1) + ".git");
                }
            }
            pullAndPush(workSpace, sourceToTargetHttpUrlMap, sourceGitlabAccessToken, targetGitlabAccessToken);
            logger.info("部分配置文件代码库更新成功!");
            result.setStatus(Status.success);
            result.setMessage("部分配置文件代码库更新成功!");
        }
        else if (param.getFeatureOption().equals(3)) {
            GroupParams groupParams = new GroupParams();
            if (!param.getRootId().equals("0")) {
                logger.info("创建子分组");
                groupParams.withParentId(Integer.valueOf(param.getRootId()))
                        .withName(param.getProjectEnName())
                        .withPath(param.getProjectEnName().toLowerCase(Locale.ROOT))
                        .withDescription(param.getProjectChName());
            } else {
                logger.info("创建顶层分组");
                groupParams.withName(param.getProjectEnName())
                        .withPath(param.getProjectEnName().toLowerCase(Locale.ROOT))
                        .withDescription(param.getProjectChName());
            }
            Group targetParentGroup = targetGitLabApi.getGroupApi().createGroup(groupParams);
            logger.info("分组创建成功");
            Integer targetParentId = targetParentGroup.getId();
            logger.info("目标分组的ID={}", targetParentId);
            Map<String, String> typeToUrlList = new HashMap<>();
            typeToUrlList.put("Backend", param.getBackendUrlList());
            typeToUrlList.put("UI", param.getFrontendUrlList());
            typeToUrlList.put("Test", param.getTestUrlList());

            for (Map.Entry<String, String> entry : typeToUrlList.entrySet()
                 ) {
                logger.info("key: {}, value: {}", entry.getKey(), entry.getValue());
                Group targetTypeGroup = targetGitLabApi.getGroupApi().createGroup(
                        new GroupParams().withName(entry.getKey()).withPath(entry.getKey().toLowerCase(Locale.ROOT)).withParentId(targetParentId));
                String pattern = "http://.*\\.git";
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(entry.getValue());
                while (m.find()) {
                    String pattern1 = "http://git\\.clamc\\.com/(.*)\\.git";
                    Pattern p1 = Pattern.compile(pattern1);
                    Matcher m1 = p1.matcher(m.group());
                    if (m1.find()) {
                        logger.info("源地址FullPath={}", m1.group(1));
                        Project sourceProject = sourceGitLabApi.getProjectApi().getProject(m1.group(1));
                        Project targetProject = targetGitLabApi.getProjectApi().createProject(
                                new Project().withNamespaceId(targetTypeGroup.getId())
                                        .withName(sourceProject.getName())
                                        .withPath(sourceProject.getPath().toLowerCase(Locale.ROOT))
                                        .withDescription(sourceProject.getDescription())
                        );
                        logger.info("已创建目标群组{}---描述为{}", targetProject.getPathWithNamespace(), targetProject.getDescription());
                        sourceToTargetHttpUrlMap.put(m.group(), targetProject.getHttpUrlToRepo());
                    }
                }
            }
            pullAndPush(workSpace, sourceToTargetHttpUrlMap, sourceGitlabAccessToken, targetGitlabAccessToken);
        }
    }

    @SneakyThrows
    public static void pullAndPush(String workSpace, Map<String, String> sourceToTargetHttpUrlMap, CredentialsProvider sourceGitlabAccessToken, CredentialsProvider targetGitlabAccessToken) {
        File workspace = new File(workSpace);
        clearWorkspace(workspace);
        int i = 1;
        for (Map.Entry<String, String> entry : sourceToTargetHttpUrlMap.entrySet()
        ) {
            File file = new File(workSpace + "/" + i);
            file.mkdirs();
            i++;
            Git git = Git.cloneRepository()
                    .setURI(entry.getKey())
                    .setDirectory(file)
                    .setCredentialsProvider(sourceGitlabAccessToken)
                    .call();
            Collection<Ref> refs = git.lsRemote()
                    .setCredentialsProvider(sourceGitlabAccessToken)
                    .setRemote("origin")
                    .setHeads(true)
                    .setTags(true)
                    .call();
            String pattern = "refs/heads/(.*)";
            Pattern p = Pattern.compile(pattern);
            for (Ref ref : refs
            ) {
                String remoteRefName = ref.getName();
                Matcher m = p.matcher(remoteRefName);
                if (m.find() && git.getRepository().getRef(remoteRefName) == null) { //本地没有的ref就去拉取
                    git.checkout()
                            .setCreateBranch(true)
                            .setName(m.group(1))
                            .setStartPoint("origin/" + m.group(1))
                            .call();
                }
            }
            if (file.listFiles().length == 1) {
                logger.error("{}是空项目", entry.getKey());
                continue;
            }
            git.push()
                    .setCredentialsProvider(targetGitlabAccessToken)
                    .setRemote(entry.getValue())
                    .setPushAll()
                    .setPushTags()
                    .setForce(true)
                    .call();
            logger.info("{} 》》》 {} Over", entry.getKey(), entry.getValue());
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
    public static void dfs(String targetParentId, GitLabApi sourceGitLabApi, GitLabApi targetGitLabApi, Group sourceRootGroup, Map<String, String> map, AtomResult result) {
        GroupParams targetGroupParams = new GroupParams();
        if (targetParentId.equals("0")) {
            targetGroupParams
                    .withName(sourceRootGroup.getName())
                    .withDescription(sourceRootGroup.getDescription())
                    .withPath(sourceRootGroup.getPath().toLowerCase(Locale.ROOT));
        } else {
            targetGroupParams.withParentId(Integer.valueOf(targetParentId))
                    .withName(sourceRootGroup.getName())
                    .withDescription(sourceRootGroup.getDescription())
                    .withPath(sourceRootGroup.getPath().toLowerCase(Locale.ROOT));
        }
        Group targetGroup;
        try {
            targetGroup = targetGitLabApi.getGroupApi().createGroup(targetGroupParams);
        } catch (GitLabApiException e) {
            logger.error("检测到目标群组已存在，请确认后手动删除，再执行同步！");
            result.setStatus(Status.failure);// 状态设置为失败
            result.setMessage("检测到目标群组已存在，请确认后手动删除，再执行同步！");
            return;
        }
        List<Group> sourceSubGroups = sourceGitLabApi.getGroupApi().getSubGroups(sourceRootGroup.getId());
        List<Project> sourceSubProjects = sourceGitLabApi.getGroupApi().getProjects(sourceRootGroup.getId());
        if (sourceSubGroups.size() == 0 && sourceSubProjects.size() == 0) {
            logger.info("当前路径下的叶子节点:源群组{}", sourceRootGroup.getFullPath());
        }
        else if (sourceSubGroups.size() != 0 && sourceSubProjects.size() == 0) {
            for (Group sg : sourceSubGroups
                 ) {
                logger.info("当前路径下只有子群组，继续追溯···{}/{}", sourceRootGroup.getFullPath(), sg.getPath());
                dfs(targetGroup.getId().toString(), sourceGitLabApi, targetGitLabApi, sg, map, result);
            }
        }
        else {
            for (Project sp : sourceSubProjects
                 ) {
                Project project = new Project();
                project.withName(sp.getName())
                        .withPath(sp.getPath().toLowerCase(Locale.ROOT))
                        .withDescription(sp.getDescription())
                        .withPublic(sp.getPublic())
                        .withVisibility(sp.getVisibility())
                        .withNamespaceId(targetGroup.getId());
                Project tp = targetGitLabApi.getProjectApi().createProject(project);
//                if (!sp.getEmptyRepo()) { // 老版本没有这个api！
//                    map.put(sp.getHttpUrlToRepo(), tp.getHttpUrlToRepo());
//                    logger.info("key:{}, value:{}", sp.getHttpUrlToRepo(), tp.getHttpUrlToRepo());
//                }
                map.put(sp.getHttpUrlToRepo(), tp.getHttpUrlToRepo());
            }
            if (sourceSubGroups.size() != 0) {
                for (Group sg : sourceSubGroups
                ) {
                    logger.info("当前路径下还有子群组，继续追溯···{}/{}", sourceRootGroup.getFullPath(), sg.getPath());
                    dfs(targetGroup.getId().toString(), sourceGitLabApi, targetGitLabApi, sg, map, result);
                }
            }
        }
    }


//    @SneakyThrows
//    public static void main(String[] args) {
//        Map<String, String> sourceToTargetHttpUrlMap = new HashMap<String, String>();
//        AtomResult result = new AtomResult();
//        String input = "http://112.126.74.102/Climb/Base/DocGeneration";
//        String rootId = "0";
//        GitLabApi sourceGitLabApi = new GitLabApi("http://112.126.74.102", "Qi3Mydedt4yKaTmADLUe");
//        GitLabApi targetGitLabApi = new GitLabApi("http://101.200.158.182", "sffSo7w2ybKCefkssJnn");
//        UsernamePasswordCredentialsProvider sourceGitlabAccessToken = new UsernamePasswordCredentialsProvider(
//                "PRIVATE-TOKEN",
//                "Qi3Mydedt4yKaTmADLUe"
//        );
//        UsernamePasswordCredentialsProvider targetGitlabAccessToken = new UsernamePasswordCredentialsProvider(
//                "PRIVATE-TOKEN",
//                "sffSo7w2ybKCefkssJnn"
//        );
//        String workspace = "C:\\Users\\chance\\Desktop\\gitlabTest";
//        String pattern = "http://112.126.74.102/(.*)";
//        Pattern r = Pattern.compile(pattern);
//        Matcher m = r.matcher(input);
//        Group sourceRootGroup;
//        if (m.find()) {
//            sourceRootGroup = sourceGitLabApi.getGroupApi().getGroup(m.group(1));
//        } else {
//            logger.error("没有匹配到源分组路径");
//            result.setStatus(Status.failure);// 状态设置为失败
//            result.setMessage("没有匹配到源分组路径");
//            return;
//        }
//        dfs(rootId, sourceGitLabApi, targetGitLabApi, sourceRootGroup, sourceToTargetHttpUrlMap, result);
//        pullAndPush(workspace, sourceToTargetHttpUrlMap, sourceGitlabAccessToken, targetGitlabAccessToken);
//    }
//@SneakyThrows
//public static void main(String[] args) {
//    String workSpace = "C:\\Users\\chance\\Desktop\\gitlabTest";
//    Map<String, String> sourceToTargetHttpUrlMap = new HashMap<String, String>();
//    GitLabApi sourceGitLabApi = new GitLabApi("http://112.126.74.102", "Qi3Mydedt4yKaTmADLUe");
//    GitLabApi targetGitLabApi = new GitLabApi("http://101.200.158.182", "sffSo7w2ybKCefkssJnn");
//    UsernamePasswordCredentialsProvider sourceGitlabAccessToken = new UsernamePasswordCredentialsProvider(
//            "PRIVATE-TOKEN",
//            "Qi3Mydedt4yKaTmADLUe"
//    );
//    UsernamePasswordCredentialsProvider targetGitlabAccessToken = new UsernamePasswordCredentialsProvider(
//            "PRIVATE-TOKEN",
//            "sffSo7w2ybKCefkssJnn"
//    );
//    String pattern = "http://.*\\.git";
//    String pattern1 = "http://112.126.74.102/(.*/(.*))\\.git";
//    Pattern p = Pattern.compile(pattern);
//    Pattern p1 = Pattern.compile(pattern1);
//    String repoUrlList = "http://112.126.74.102/Climb/Support/support1.git\nhttp://112.126.74.102/Climb/Support/support2.git";
//    Matcher m = p.matcher(repoUrlList);
//    while (m.find()) {
//        Matcher m1 = p1.matcher(m.group());
//        if (m1.find()) {
//            Project sp = sourceGitLabApi.getProjectApi().getProject(m1.group(1));
//            Project tp = new Project().withName(sp.getName())
//                    .withDescription(sp.getDescription())
//                    .withPath(sp.getPath().toLowerCase(Locale.ROOT))
//                    .withNamespaceId(Integer.parseInt("108"));
//            Project targetProject;
//            try {
//                targetProject = targetGitLabApi.getProjectApi().createProject(tp);
//            } catch (GitLabApiException e) {
//                logger.error("目标项目已存在");
//                Group targetRootGroup = targetGitLabApi.getGroupApi().getGroup(Integer.parseInt("108"));
//                targetProject = targetGitLabApi.getProjectApi().getProject(targetRootGroup.getFullPath() + "/" + m1.group(2));
//            }
//            sourceToTargetHttpUrlMap.put(sp.getHttpUrlToRepo(), targetProject.getHttpUrlToRepo());
//        }
//    }
//    pullAndPush(workSpace, sourceToTargetHttpUrlMap, sourceGitlabAccessToken, targetGitlabAccessToken);
//}
//    @SneakyThrows
//    public static void main(String[] args) {
//        Map<String, String> sourceToTargetHttpUrlMap = new HashMap<String, String>();
//        AtomResult result = new AtomResult();
//        String input = "http://112.126.74.102/ClimbConfig";
//        String rootId = "0";
//        GitLabApi sourceGitLabApi = new GitLabApi("http://112.126.74.102", "Qi3Mydedt4yKaTmADLUe");
//        GitLabApi targetGitLabApi = new GitLabApi("http://101.200.158.182", "sffSo7w2ybKCefkssJnn");
//        UsernamePasswordCredentialsProvider sourceGitlabAccessToken = new UsernamePasswordCredentialsProvider(
//                "PRIVATE-TOKEN",
//                "Qi3Mydedt4yKaTmADLUe"
//        );
//        UsernamePasswordCredentialsProvider targetGitlabAccessToken = new UsernamePasswordCredentialsProvider(
//                "PRIVATE-TOKEN",
//                "sffSo7w2ybKCefkssJnn"
//        );
//        String workspace = "C:\\Users\\chance\\Desktop\\gitlabTest";
//        Group climbConfig = sourceGitLabApi.getGroupApi().getGroup("ClimbConfig");
//        dfs("0", sourceGitLabApi, targetGitLabApi, climbConfig, sourceToTargetHttpUrlMap, result);
//        pullAndPush(workspace, sourceToTargetHttpUrlMap, sourceGitlabAccessToken, targetGitlabAccessToken);
//    }

    @SneakyThrows
    public static void main(String[] args) {
        String workSpace = "C:\\Users\\chance\\Desktop\\gitlabTest";
        Map<String, String> sourceToTargetHttpUrlMap = new HashMap<String, String>();
        GitLabApi sourceGitLabApi = new GitLabApi("http://112.126.74.102", "Qi3Mydedt4yKaTmADLUe");
        GitLabApi targetGitLabApi = new GitLabApi("http://101.200.158.182", "sffSo7w2ybKCefkssJnn");
        UsernamePasswordCredentialsProvider sourceGitlabAccessToken = new UsernamePasswordCredentialsProvider(
                "PRIVATE-TOKEN",
                "Qi3Mydedt4yKaTmADLUe"
        );
        UsernamePasswordCredentialsProvider targetGitlabAccessToken = new UsernamePasswordCredentialsProvider(
                "PRIVATE-TOKEN",
                "sffSo7w2ybKCefkssJnn"
        );
        String repoUrlList = "http://112.126.74.102/ClimbConfig/test1.git\nhttp://112.126.74.102/ClimbConfig/test2.git";
        String pattern = "http://.*\\.git";
        String pattern1 = "http://112.126.74.102/.*/(.*)\\.git";
        Pattern p = Pattern.compile(pattern);
        Pattern p1 = Pattern.compile(pattern1);
        Matcher m = p.matcher(repoUrlList);
        while (m.find()) {
            logger.info("1");
            Matcher m1 = p1.matcher(m.group());
            if (m1.find()) {
                logger.info("2");
                sourceToTargetHttpUrlMap.put(m.group(), "http://101.200.158.182/climbconfig/" + m1.group(1) + ".git");
            }
        }
        pullAndPush(workSpace, sourceToTargetHttpUrlMap, sourceGitlabAccessToken, targetGitlabAccessToken);
    }

}
