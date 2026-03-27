# Git Push Helper

Files in this folder:

- `push-to-git.bat`: double-click to stage, commit, and push the current branch

How to use:

1. Double-click `push-to-git.bat`
2. Wait for the script to finish

What the script does:

1. Runs `git add -A`
2. Runs `git commit -m "auto sync <date> <time>"`
3. Runs `git push -u origin <current-branch>`

Notes:

- Current remote: `origin`
- Current default branch detected when created: `main`
- If there are no changes, the script exits without creating a commit
- If push fails because the remote is ahead, pull/rebase first, resolve any conflicts, and then run the script again

Common issue:

- If the script asks for your SSH passphrase or GitHub authentication, complete that prompt and the push will continue
