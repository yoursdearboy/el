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

# Formatting

Date format set using `:el/date-format` context key, by default it is `yyyy-MM-dd`.

For more flexible formatting use function `(format date format-string)`.
It's a good idea to pass the format string using a context key.
