package activiti;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.FormService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricFormProperty;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricVariableUpdate;
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
	public void runDemo() {
		//发布工作流
//		this.deploy();
		//查询流程信息
		System.out.println("--------------------查询流程信息---------------------");
		this.queryWorkFlow();
		//启动流程
		System.out.println("--------------------启动流程---------------------");
		this.startWorkflow();
		//查询未完成的流程
		System.out.println("--------------------查询未完成的流程---------------------");
		this.createUnFinishedProcessInstanceQuery();
		//流程处理
		System.out.println("--------------------流程处理---------------------");
		this.processTask();
		//查询完成的流程
		System.out.println("--------------------查询完成的流程---------------------");
		this.createFinishedProcessInstanceQuery();
		System.out.println("-----------------------------");
		this.createUnFinishedProcessInstanceQuery();
		System.out.println("--------------------读取历史变量---------------------------------");
		
	}
	
	public void deploy() {
		System.out.println("------------------------------------------------------");
		Assert.assertNotNull(processEngine);
		logger.info(processEngine.toString());
		System.out.println("deploy workflow...");
		RepositoryService repositoryService = processEngine.getRepositoryService();

		repositoryService.createDeployment().name(bpmName)
				.addZipInputStream(new ZipInputStream(this.getClass().getClassLoader().getResourceAsStream("deployments/" + bpmName + ".zip"))).deploy();
		
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
		FormService service = processEngine.getFormService();
		Map<String, String> variables = new HashMap<String, String>();
		variables.put("id", "1");
		variables.put("userId", "test4");
		variables.put("wasteTransNum", "2.3");
		variables.put("landfillHandler", "3.3");
		variables.put("compostHandler", "4.3");
		variables.put("burnHandler", "5.3");
		variables.put("harmlessHandler", "6.3");
		variables.put("transOutsideHarmlessHandler", "7.3");
		variables.put("rualWasteHandler", "12.3");
		variables.put("outsideWasteHandler", "22.3");
		variables.put("comprehensiveHandler", "12.3");
		System.out.println("start workflow by " + variables.get("userId"));
		ProcessDefinition processDefinition = processEngine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(bpmName).singleResult();
		ProcessInstance instance = service.submitStartFormData(processDefinition.getId(), variables);
		Assert.assertNotNull(instance);
		System.out.println("user\tproInsId\tdeployId");
		System.out.println(variables.get("userId") + "\t" + instance.getId() + "\tpdid:"
				+ instance.getProcessDefinitionId());
		this.readWorkflowPicture(instance.getProcessDefinitionId(), instance.getId(), variables.get("userId")
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
			this.readFormData(task.getId());
			Map<String, String> variables = new HashMap<String, String>();
			variables.put("approve", "true");
			processEngine.getFormService().submitTaskFormData(task.getId(), variables);
			System.out.println(task.getId() + "\t" + task.getName() + "\t" + task.getProcessInstanceId() + "\t"
					+ task.getProcessDefinitionId());
			String fileName = userId + "_deal_with_task+" + task.getId();
			if (userId != "jackchen") {
				try {
					if (variables.get("approve")=="false") {
						this.readWorkflowPicture(task.getProcessDefinitionId(), task.getProcessInstanceId(), fileName);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
			if (variables.get("approve")=="false") {
				this.readPackageVariables(task.getProcessInstanceId());
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
//		this.processTask("designer");
		this.processTask("admin");
	}

	private void readWorkflowPicture(String prodefinId, String instanceId, String fileName) {
		System.out.println("------------------------------------------------------");
		RuntimeService runtimeService = processEngine.getRuntimeService();
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
		InputStream in = this.getDiagram(instanceId);
		productImage(in,fileName);

	}

	private void productImage(InputStream inputStream,String fileName) {
		FileOutputStream out = null;
		File picture = new File("src/test/java/"+fileName + ".png");
		try {
			byte[] data = new byte[inputStream.available()];
			out = new FileOutputStream(picture);
			inputStream.read(data);
			out.write(data);
//			System.out.println(new sun.misc.BASE64Encoder().encode(data));
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
		for (ProcessDefinition processDefinition : list) {
			HistoricProcessInstanceQuery finishedQuery = processEngine.getHistoryService()
					.createHistoricProcessInstanceQuery().processDefinitionKey(processDefinition.getKey()).finished();
			List<HistoricProcessInstance> historicProcessInstances = finishedQuery.list();
			for (HistoricProcessInstance instance : historicProcessInstances) {
				System.out.println(instance.getName() + "\t" + instance.getStartUserId());
			}
		}

	}

	public void createUnFinishedProcessInstanceQuery() {
		ProcessDefinitionQuery query = processEngine.getRepositoryService().createProcessDefinitionQuery();
		query.processDefinitionKey(bpmName);
		List<ProcessDefinition> list = query.list();
		for (ProcessDefinition processDefinition : list) {
			ProcessInstanceQuery unfinishedQuery = processEngine.getRuntimeService().createProcessInstanceQuery()
					.processDefinitionKey(processDefinition.getKey()).active();
			List<ProcessInstance> historicProcessInstances = unfinishedQuery.list();
			for (ProcessInstance instance : historicProcessInstances) {
				System.out.println(instance.getActivityId() + "\t" + instance.getDeploymentId() + "\t"
						+ instance.getProcessInstanceId());
			}
		}

	}
	
	private Map<String, Object> readPackageVariables(String instanceId) {
		System.out.println("--------------------读取历史变量---------------------------------");
		Map<String, Object> historyVariables = new HashMap<String, Object>();
		List<HistoricDetail> list = processEngine.getHistoryService().createHistoricDetailQuery().processInstanceId(instanceId).list();
		for(HistoricDetail detail:list){
			if(detail instanceof HistoricFormProperty){
				HistoricFormProperty field = (HistoricFormProperty) detail;
				historyVariables.put(field.getPropertyId(), field.getPropertyValue());
				System.out.println("form field : taskId="+field.getTaskId()+"\t"+field.getPropertyId()+"="+field.getPropertyValue());
			}else if (detail instanceof HistoricVariableUpdate) {
				HistoricVariableUpdate variable = (HistoricVariableUpdate) detail;
				historyVariables.put(variable.getVariableName(), variable.getValue());
				System.out.println("variable :"+variable.getVariableName()+"="+variable.getValue());
			}
		}
		this.printData(historyVariables);
		return historyVariables;
	}
	
	private Map<String, Object> readFormData(String taskId) {
		System.out.println("----------------------------读取表单数据----------------------------------------");
		TaskFormData formData = processEngine.getFormService().getTaskFormData(taskId);
		List<FormProperty> formProperties = formData.getFormProperties();
		Map<String, Object> formValue = new HashMap<String, Object>();
		for(FormProperty property:formProperties){
			formValue.put(property.getId(), property.getValue());
		}
		this.printData(formValue);
		return formValue;
	}
	
	private void printData(Map<String, Object> datas) {
		System.out.println("---------------------------------打印输出---------------------------------------");
		for(String name:datas.keySet()){
			System.out.println(name+"="+datas.get(name));
		}
	}
}
