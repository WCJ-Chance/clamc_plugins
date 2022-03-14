package com.tencent.bk.devops.atom.task;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import com.tencent.bk.devops.atom.task.pojo.AtomParam;
import com.tencent.bk.devops.atom.utils.json.JsonUtil;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @version 1.0
 */
@AtomService(paramClass = AtomParam.class)
public class MergeRequestAtom implements TaskAtom<AtomParam> {

    private final static Logger logger = LoggerFactory.getLogger(MergeRequestAtom.class);

    /**
     * 执行主入口
     *
     * @param atomContext 插件上下文
     */
    @SneakyThrows
    @Override
    public void execute(AtomContext<AtomParam> atomContext) throws IOException {
        // 1.1 拿到请求参数
        AtomParam param = atomContext.getParam();
        logger.info("the param is :{}", JsonUtil.toJson(param));
        // 1.2 拿到初始化好的返回结果对象
        AtomResult result = atomContext.getResult();
        // 2. 校验参数失败直接返回
        checkParam(param, result);
        if (result.getStatus() != Status.success) {
            return;
        }
        // 3. 模拟处理插件业务逻辑
        UsernamePasswordCredentialsProvider gitlabAccessToken = new UsernamePasswordCredentialsProvider(
                "PRIVATE-TOKEN",
                atomContext.getSensitiveConfParam("gitlabAccessToken")
        );
//        Collection<String> branchesToClone = new ArrayList<>();
//        branchesToClone.add(param.getSourceBranch());
//        branchesToClone.add(param.getTargetBranch());
        File workSpace = new File(param.getBkWorkspace());
        clearWorkspace(workSpace);
        logger.info("Cloning from " + param.getGitUrl() + " to " + param.getBkWorkspace());
        Git git = Git.cloneRepository()
                     .setURI(param.getGitUrl())
                     .setDirectory(workSpace)
                     .setCredentialsProvider(gitlabAccessToken)
                     .call();
        logger.info("Having repository: {}", git.getRepository().getDirectory());
        logger.info("Starting pull source branch···");
        git.checkout()
                .setName(param.getSourceBranch())
                .setCreateBranch(true)
                .setStartPoint("origin/" + param.getSourceBranch())
                .call();
        git.pull()
                .setCredentialsProvider(gitlabAccessToken)
                .setRemote("origin")
                .call();
        logger.info("Listing local branches:");
        List<Ref> refs = git.branchList().call();
        for (Ref ref : refs
        ) {
            logger.info(ref.getName());
        }
        Repository repository = new FileRepository(param.getBkWorkspace() + "/.git");
        logger.info("Switching to target branch:");
        git.checkout().setName(param.getTargetBranch()).call();
        logger.info("Current branch is {}", git.getRepository().getFullBranch());
        ObjectId mergeBase = repository.resolve(param.getSourceBranch());
        logger.info("Starting merge···");
        MergeResult mergeResult = git.merge()
                .include(mergeBase)
                .setCommit(true)
                .setFastForward(MergeCommand.FastForwardMode.NO_FF)
                .setMessage("Merged " + param.getSourceBranch())
                .call();
        logger.info("Merge-result for id:{} is {}", mergeBase, mergeResult);
        if (mergeResult.getConflicts() != null) {
            for (Map.Entry<String,int[][]> entry : mergeResult.getConflicts().entrySet()) {
                logger.error("Key: {}", entry.getKey());
                for(int[] arr : entry.getValue()) {
                    logger.error("value: {}", Arrays.toString(arr));
                }
            }
        }
        if (!mergeResult.getMergeStatus().isSuccessful()) {
            result.setStatus(Status.failure);
            result.setMessage(mergeResult.getMergeStatus().name());
            return;
        }
        git.push()
                .setRemote("origin")
                .add(param.getTargetBranch())
                .setCredentialsProvider(gitlabAccessToken)
                .call();
    }

    /**
     * 检查参数
     *
     * @param param  请求参数
     * @param result 结果
     */
    private void checkParam(AtomParam param, AtomResult result) {
        // 参数检查
//        if (StringUtils.isBlank(param.getBasicPath())) {
//            result.setStatus(Status.failure);// 状态设置为失败
//            result.setMessage("搜索目录不能为空!"); // 失败信息回传给插件执行框架会打印出结果
//        }
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
        UsernamePasswordCredentialsProvider gitlabAccessToken = new UsernamePasswordCredentialsProvider("PRIVATE-TOKEN", "nnancB_5N5tBxXE7Kgc_");
        File workSpace = new File("C:\\Users\\chancew\\Desktop\\gitTutorial");
        clearWorkspace(workSpace);
        Git git = Git.cloneRepository()
                .setDirectory(workSpace)
                .setCredentialsProvider(gitlabAccessToken)
                .setURI("https://code.canway.net/soft-fy21-clamc-devops/pipeline-plugin.git")
                .call();
        Collection<Ref> remoteRefs = git.lsRemote()
                .setRemote("origin")
                .setHeads(true)
                .setTags(false)
                .setCredentialsProvider(gitlabAccessToken)
                .call();
        for (Ref remoteRef: remoteRefs
             ) {
            if (remoteRef.getName().substring(11).equals("stage/v1.0.0")) {
                git.checkout().setName("stage/v1.0.0")
                        .setCreateBranch(true)
                        .setStartPoint("origin/stage/v1.0.0")
                        .call();
            }
        }
        git.pull()
                .setCredentialsProvider(gitlabAccessToken)
                .setRemote("origin")
                .call();
    }

}
