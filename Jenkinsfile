@Library('shared-ci-cd')_

echo env.BRANCH_NAME
echo env.CHANGE_BRANCH
echo env.CHANGE_ID

if(env.BRANCH_NAME=='main') { //if push to main branch 
    sharedPipelineJavaMain{}
}else if (env.CHANGE_BRANCH != 'main' && env.CHANGE_ID != null){ //if pull request
    sharedPipelineJavaPull{}
} else{
    sharedPipelineJavaDev{} //if push to other branch 
}


