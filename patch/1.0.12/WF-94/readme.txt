Summary

    * Status: exo:PublishingProcess action is not executed
    * CCP Issue: N/A, Product Jira Issue: WF-94.
    * Fixes also: ECM-5583
    * Complexity: Normal

The Proposal
Problem description

What is the problem to fix?

    * Go to Site Explorer → Site Management
    * Select 1 folder & add document
    * Select added document to view
    * Choose Manage Action in Admin tab
    * Choose exo:PublishingProcess for Action type
    * Input valid values for other fields
    * Click Save
    * Right click on folder & select the name of has just been added action
    * Go to WF Controller page --> no request is displayed

Fix description

How is the problem fixed?

    * When displaying the tasks in WF to manage, we forgot to display the tasks belonging to "any" (*) membership. So to fix this problem, we add the task belonging to "any" into the display list.

Patch file: WF-94.patch

Tests to perform

Reproduction test

    * Go to Site Explorer → Site Management
    * Select 1 folder & add document
    * Select added document to view
    * Choose Manage Action in Admin tab
    * Choose exo:PublishingProcess for Action type
    * Input valid values for other fields
    * Click Save
    * Right click on folder & select the name of has just been added action
    * Go to WF Controller page --> no request is displayed

Tests performed at DevLevel
After fixing:

    * Run DMS with Workflow.
    * Go to Site Explorer → Site Management
    * Select 1 folder & add document
    * Select added document to view
    * Choose Manage Action in Admin tab
    * Choose exo:PublishingProcess for Action type
    * Input valid values for other fields
    * Click Save
    * Right click on folder & select the name of has just been added action
    * Go to WF Controller page --> request of newly created document is displayed.

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:

    * No

Configuration changes

Configuration changes:

    * No

Will previous configuration continue to work?

    * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change: no

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
* Patch validated.

Support Comment
* Patch validated.

QA Feedbacks
*

