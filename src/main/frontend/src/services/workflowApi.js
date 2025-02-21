createWorkflow: (workflowData) => {
  // Format the workflow data to match ApprovalWorkflowDTO
  const formattedWorkflow = {
    name: workflowData.name,
    description: workflowData.description,
    type: workflowData.type,
    active: true,
    steps: workflowData.steps?.map(step => ({
      stepOrder: step.stepOrder || 0,
      name: step.name,
      description: step.description,
      approvalThreshold: workflowData.type === 'PERCENTAGE_APPROVAL' ? step.approvalThreshold : null,
      approverIds: Array.isArray(step.approverIds) ? step.approverIds : [step.approverIds],
      active: true
    }))
  };

  console.log('Creating workflow with data:', formattedWorkflow);

  // Send the request without userId header since it's in the JWT token
  return api.post('/workflows', formattedWorkflow)
    .catch(error => {
      console.error('Workflow creation error:', {
        status: error.response?.status,
        data: error.response?.data,
        message: error.message
      });
      throw error;
    });
}, 