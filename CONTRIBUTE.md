With Unstoppable we aim to create a mainstream-friendly multi-blockchain wallet implementation on iOS and Android platforms. 

We invite you to review, improve and reuse our codebase. If you are looking to contribute please familiarize yourself with the goals of the Unstoppable Wallet project. 

We are sailing towards an open-source wallet that:

- is actively maintained and improved by interested developers worldwide. The wallet should ultimately support all widely adopted blockchain standards (i.e essential Bitcoin BIPs) and move in parallel with blockchain protocol updates.

- can unconditionally be reused for other blockchain projects. For instance, the wallet implementation in the Unstoppable app can be used as under the hood authentication/identity component with inherent account backup/restore mechanisms for any decentralized app.

- always operates in a trustless manner, given that underlying blockchain protocols provide such capability in practical terms. The wallet should not rely on any data returned from a 3rd party server and to the extent that’s possible verify that data independently.

- is expandable to support new blockchains without significant effort while keeping the technical debt at manageable levels. It should be possible to integrate new blockchains in a modularized way without adding to the complexity.

-  can always operate in a censorship-resistant and failure-free manner. The core wallet features i.e. crypto transfers or blockchain sync should always remain functional, even in cases where there is a deliberate attempt by someone to censor the wallet.

- operates in a private manner, if the underlying blockchain protocol provides such capability. The wallet should have capability for the client to mask all outgoing/incoming traffic to make it impossible for any third party to obtain any identifiable data such as IP address, transaction history, etc.

- adheres to the commonly accepted best practices (like test coverage) when it comes to the application architecture. 

Keeping above in mind we are looking forward for your input. For the sake of keeping the process technically objective our willingness to merge (or not to) some contributions is going to be purely based on our technical assessment of the submitted code and its impact on the existing codebase. While it’s in our best interests to make the contribution process as straightforward as possible it’s of paramount importance for us to keep  the project complexity at a level we are comfortable with so we can move fast.

If you’re a submitting a PR with the intent to extend wallet support to additional blockchains or tokens the Horizontal Systems team is going to primarily look at how the added code interacts with the existing components and potential technical and usability issues that may arise as a result. If the PR merge is likely to increase chances of app crashes or inconsistent user behavior in the user interface we are likely to reject it. Therefore, before you jump on PR it’s important for your to get familiar with the integration of currently supported blockchains. It goes without saying that you can always fork the wallet and proceed as you wish.

## Code Contribution

### Terminology

* **Working development branch**. It matches the pattern "version/X.Y.Z", where X.Y.Z the version of app we're working on at the time. Usually there is only one working branch

Please follow these steps

1. Clone the current working development branch.
1. Make changes in your cloned version. When making changes please make sure that you follow the style-guides
1. Send the Pull Request to the current working development branch.

### Pull Requests

Please follow these steps when sending pull requests

1. Choose the current working development branch as base branch for Pull Request
1. Make reference to the related issue if any in the description

### Style-guides

#### Git Commit Messages

https://chris.beams.io/posts/git-commit/#seven-rules

Peace!
