package activiti;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("deprecation")
@RunWith(value = SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:activiti/spring.activiti.xml" })
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
@Transactional
public class ActivitiDemo extends AbstractTransactionalJUnit4SpringContextTests {
	@Autowired
	private ProcessEngine processEngine;

	private Logger logger = LoggerFactory.getLogger(ActivitiDemo.class);
	private String bpmName = "projectReport";

	@Before
	public void setup() {

	}

	@Test
	public void deploy() {
		System.out.println("------------------------------------------------------");
		Assert.assertNotNull(processEngine);
		logger.info(processEngine.toString());
		System.out.println("deploy workflow...");
		RepositoryService repositoryService = processEngine.getRepositoryService();

		repositoryService
				.createDeployment()
				.name(bpmName)
				.addZipInputStream(
						new ZipInputStream(this.getClass().getClassLoader()
								.getResourceAsStream("deployments/" + bpmName + ".zip"))).deploy();
		this.queryWorkFlow();
		this.startWorkflow();
		this.processTask();
		this.createFinishedProcessInstanceQuery();
		System.out.println("-----------------------------");
		this.createUnFinishedProcessInstanceQuery();
	}

	// @Test
	public void queryWorkFlow() {
		System.out.println("------------------------------------------------------");
		RepositoryService repositoryService = processEngine.getRepositoryService();
		ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
		query.processDefinitionKey(bpmName);
		List<ProcessDefinition> list = query.list();
		System.out.println("Leave workflow:");
		System.out.println("Id\t\tdeployId\tname\t\tversion");
		for (ProcessDefinition pd : list) {
			System.out.println(pd.getId() + "\t" + pd.getDeploymentId() + "\t\t" + pd.getName() + "\t"
					+ pd.getVersion());
		}
	}

	// @Test
	public void startWorkflow() {
		System.out.println("------------------------------------------------------");
		RuntimeService service = processEngine.getRuntimeService();
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("startTime", "2016-5-3");
		variables.put("endTime", "2016-5-5");
		variables.put("applyUser", "test4");
		System.out.println("start workflow by " + variables.get("applyUser"));
		ProcessInstance instance = service.startProcessInstanceByKey(bpmName, variables);
		Assert.assertNotNull(instance);
		System.out.println("user\tproInsId\tdeployId");
		System.out.println(variables.get("applyUser") + "\t" + instance.getId() + "\tpdid:"
				+ instance.getProcessDefinitionId());
		this.readWorkflowPicture(instance.getProcessDefinitionId(), instance.getId(), variables.get("applyUser")
				.toString());
	}

	private void processTask(String userId) {
		TaskService taskService = processEngine.getTaskService();
		List<Task> tasks = taskService.createTaskQuery().taskCandidateUser(userId).list();
		// 接收�?有任�?
		System.out.println(userId + "接收任务");
		System.out.println("taskId\ttaskName\tinstanceId\tpdId");
		for (Task task : tasks) {
			taskService.claim(task.getId(), userId);
			System.out.println(task.getId() + "\t" + task.getName() + "\t" + task.getProcessInstanceId() + "\t"
					+ task.getProcessDefinitionId());
			String fileName = userId + "_claim_task_" + task.getId();
			this.readWorkflowPicture(task.getProcessDefinitionId(), task.getProcessInstanceId(), fileName);
		}
		// 处理本人�?有的任务
		System.out.println(userId + "处理任务");
		System.out.println("taskId\ttaskName\tinstanceId\tpdId");
		for (Task task : tasks) {
			Map<String, Object> variables = new HashMap<String, Object>();
			variables.put("approved", true);
			taskService.complete(task.getId(), variables);
			System.out.println(task.getId() + "\t" + task.getName() + "\t" + task.getProcessInstanceId() + "\t"
					+ task.getProcessDefinitionId());
			String fileName = userId + "_deal_with_task+" + task.getId();
			if (userId != "jackchen") {
				this.readWorkflowPicture(task.getProcessDefinitionId(), task.getProcessInstanceId(), fileName);
			}

		}
	}

	public InputStream getDiagram(String processInstanceId) {
		// 查询流程实例
		RuntimeService runtimeService = processEngine.getRuntimeService();
		RepositoryService repositoryService = processEngine.getRepositoryService();
		ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId)
				.singleResult();
		BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
		// 得到正在执行的环节
		List<String> activeIds = runtimeService.getActiveActivityIds(pi.getId());
		InputStream inputStream = new DefaultProcessDiagramGenerator().generateDiagram(bpmnModel, "png", activeIds,
				Collections.<String> emptyList(), processEngine.getProcessEngineConfiguration().getActivityFontName(),
				processEngine.getProcessEngineConfiguration().getLabelFontName(), null, 1.0);
		return inputStream;
	}

	// @Test
	public void processTask() {
		this.processTask("designer");
		this.processTask("admin");
	}

	private void readWorkflowPicture(String prodefinId, String instanceId, String fileName) {
		System.out.println("------------------------------------------------------");
		RepositoryService repositoryService = processEngine.getRepositoryService();
		RuntimeService runtimeService = processEngine.getRuntimeService();
		BpmnModel bpmnModel = repositoryService.getBpmnModel(prodefinId);
		ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
		List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().processDefinitionId(prodefinId)
				.list();
		boolean exists = false;
		for (ProcessInstance instance : instances) {
			if (instanceId == instance.getId()) {
				exists = true;
				break;
			}
		}
		Assert.assertFalse(exists);
		List<String> activeActivityIds = runtimeService.getActiveActivityIds(instanceId);
		for (String id : activeActivityIds) {
			System.out.println(id);
		}
		System.out.println(processEngine.getProcessEngineConfiguration().getActivityFontName());
		InputStream in = this.getDiagram(instanceId);
//		InputStream in = new DefaultProcessDiagramGenerator().generateDiagram(bpmnModel, "png", activeActivityIds);
		productImage(in);

	}
	
	private void productImage(InputStream inputStream) {
		FileOutputStream out = null;
		File picture = new File("src/test/java/report_audit" + ".png");
		try {
			byte[] data = new byte[inputStream.available()];
			out = new FileOutputStream(picture);
			inputStream.read(data);
			out.write(data);
			System.out.println(new sun.misc.BASE64Encoder().encode(data));
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void createFinishedProcessInstanceQuery() {
		ProcessDefinitionQuery query = processEngine.getRepositoryService().createProcessDefinitionQuery();
		query.processDefinitionKey(bpmName);
		List<ProcessDefinition> list = query.list();
		for (ProcessDefinition processDefinition :list) {
			HistoricProcessInstanceQuery finishedQuery = processEngine.getHistoryService().createHistoricProcessInstanceQuery()
		            .processDefinitionKey(processDefinition.getKey()).finished();
			List<HistoricProcessInstance> historicProcessInstances = finishedQuery.list();
			for(HistoricProcessInstance instance:historicProcessInstances){
				System.out.println(instance.getName()+"\t"+instance.getStartUserId());
			}
		}
	    
	}
	
	public void createUnFinishedProcessInstanceQuery() {
		ProcessDefinitionQuery query = processEngine.getRepositoryService().createProcessDefinitionQuery();
		query.processDefinitionKey(bpmName);
		List<ProcessDefinition> list = query.list();
		for (ProcessDefinition processDefinition :list) {
			ProcessInstanceQuery unfinishedQuery = processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionKey(processDefinition.getKey())
		            .active();
			List<ProcessInstance> historicProcessInstances = unfinishedQuery.list();
			for(ProcessInstance instance:historicProcessInstances){
				System.out.println(instance.getActivityId()+"\t"+instance.getDeploymentId()+"\t"+instance.getProcessInstanceId());
			}
		}
		
	    
	}
}
