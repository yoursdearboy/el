# *el*

Enlive templates Thymeleaf way.

```clj
(el/template "template.html" {:x 1 :records [{:x 2} {:x 3} {:x 4}]})
```

```html
<p :el:if="(> x 0)">I told u - ${x} > 0</p>
<div>
    <h1>Other great numbers</h1>
    <table>
    <tbody :el:table="records">
        <tr>
            <td>${x}</td>
        </tr>
    </tbody>
    </table>
</div>
```
