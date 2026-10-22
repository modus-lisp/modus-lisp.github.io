;;;; The layout engine for bin/graph: trellis, the Common Lisp layered layout
;;;; after Eclipse ELK, loaded from its sibling clone in the workspace.  Reads ELK
;;;; JSON on stdin, writes the laid-out graph as ELK JSON on stdout.
;;;;
;;;;   sbcl --script tools/trellis/layout.lisp WORKSPACE < graph.json

(require :asdf)
(let ((workspace (pathname (concatenate 'string (string-right-trim "/" (second sb-ext:*posix-argv*)) "/"))))
  (dolist (repo '("trellis" "json-simple"))
    (push (merge-pathnames (concatenate 'string repo "/") workspace) asdf:*central-registry*)))
(let ((*standard-output* (make-broadcast-stream))
      (*error-output* (make-broadcast-stream)))
  (asdf:load-system :trellis))
(write-string
 (funcall (find-symbol "LAYOUT-JSON" "TRELLIS")
          (with-output-to-string (out)
            (loop for line = (read-line *standard-input* nil)
                  while line do (write-line line out)))))
