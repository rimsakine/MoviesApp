from pathlib import Path
from datetime import date
from PIL import Image, ImageDraw, ImageFont
from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "Rapport_MoviesApp"
ASSET_DIR = ROOT / "report_assets"
OUT_DIR.mkdir(exist_ok=True)
ASSET_DIR.mkdir(exist_ok=True)

DOCX_PATH = OUT_DIR / "Rapport_MoviesApp_Rim_Sakine.docx"
DOCX_PATH_DETAILED = OUT_DIR / "Rapport_MoviesApp_EMSI_Rim_Sakine_structure_detaillee.docx"
DOCX_PATH_PRO = OUT_DIR / "Rapport_MoviesApp_EMSI_Rim_Sakine_30_pages_design_pro.docx"
LOGO_PATH = ASSET_DIR / "logo-emsi.png"

NAVY = RGBColor(11, 37, 69)
BLUE = RGBColor(46, 116, 181)
DARK_BLUE = RGBColor(31, 77, 120)
RED = RGBColor(229, 9, 20)
GRAY = RGBColor(90, 90, 90)
LIGHT_GRAY = "F2F4F7"
LIGHT_BLUE = "E8EEF5"


def font_path():
    candidates = [
        r"C:\Windows\Fonts\arial.ttf",
        r"C:\Windows\Fonts\calibri.ttf",
        r"C:\Windows\Fonts\segoeui.ttf",
    ]
    for c in candidates:
        if Path(c).exists():
            return c
    return None


FONT = font_path()


def get_font(size=26, bold=False):
    if FONT:
        return ImageFont.truetype(FONT, size=size)
    return ImageFont.load_default()


def draw_box(draw, xy, text, fill="#F7F9FC", outline="#2E74B5", text_fill="#0B2545", radius=16, size=24):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=3)
    x1, y1, x2, y2 = xy
    font = get_font(size)
    lines = wrap_text(text, font, max_width=x2 - x1 - 32)
    line_h = size + 8
    total_h = line_h * len(lines)
    y = y1 + ((y2 - y1) - total_h) / 2
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=font)
        draw.text((x1 + ((x2 - x1) - (bbox[2] - bbox[0])) / 2, y), line, font=font, fill=text_fill)
        y += line_h


def wrap_text(text, font, max_width):
    words = text.split()
    lines = []
    current = ""
    for word in words:
        trial = word if not current else current + " " + word
        width = font.getbbox(trial)[2]
        if width <= max_width:
            current = trial
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def arrow(draw, start, end, color="#596B7A", width=4):
    draw.line([start, end], fill=color, width=width)
    x1, y1 = start
    x2, y2 = end
    import math
    angle = math.atan2(y2 - y1, x2 - x1)
    length = 15
    for delta in (2.65, -2.65):
        x = x2 - length * math.cos(angle + delta)
        y = y2 - length * math.sin(angle + delta)
        draw.line([(x2, y2), (x, y)], fill=color, width=width)


def save_architecture_diagram(path):
    img = Image.new("RGB", (1600, 900), "#FFFFFF")
    d = ImageDraw.Draw(img)
    title_font = get_font(34)
    d.text((50, 40), "Architecture generale de MoviesApp", fill="#0B2545", font=title_font)
    draw_box(d, (80, 190, 420, 360), "Application Android Java\nMoviesApp", fill="#FFF4F4", outline="#E50914")
    services = [
        ((620, 100, 940, 220), "TMDB API\nfilms, images, casting"),
        ((620, 260, 940, 380), "Firebase\nAuth + Firestore"),
        ((620, 420, 940, 540), "Supabase Storage\nphoto de profil"),
        ((620, 580, 940, 700), "Google Maps + Places\ncinemas proches"),
    ]
    for xy, text in services:
        draw_box(d, xy, text, fill="#F7F9FC", outline="#2E74B5", size=22)
        arrow(d, (420, 275), (620, (xy[1] + xy[3]) // 2), color="#596B7A")
    draw_box(d, (1120, 300, 1480, 470), "Serveur Node.js Express\nCineBot API", fill="#F4F6F9", outline="#1F4D78")
    arrow(d, (420, 275), (1120, 385), color="#596B7A")
    draw_box(d, (1120, 560, 1480, 700), "Modele chatbot local\nrecommandations marocaines", fill="#F8FBF8", outline="#1E8E3E")
    arrow(d, (1300, 470), (1300, 560), color="#596B7A")
    img.save(path)


def save_use_case_diagram(path):
    img = Image.new("RGB", (1600, 950), "#FFFFFF")
    d = ImageDraw.Draw(img)
    d.text((50, 35), "Diagramme de cas d'utilisation", fill="#0B2545", font=get_font(34))
    d.ellipse((80, 190, 150, 260), outline="#0B2545", width=4)
    d.line((115, 260, 115, 420), fill="#0B2545", width=4)
    d.line((60, 320, 170, 320), fill="#0B2545", width=4)
    d.line((115, 420, 60, 520), fill="#0B2545", width=4)
    d.line((115, 420, 170, 520), fill="#0B2545", width=4)
    d.text((55, 545), "Utilisateur", fill="#0B2545", font=get_font(24))
    d.rounded_rectangle((300, 100, 1500, 850), radius=24, outline="#2E74B5", width=4)
    d.text((345, 125), "Systeme MoviesApp", fill="#2E74B5", font=get_font(30))
    cases = [
        (430, 220, "S'inscrire / se connecter"),
        (850, 220, "Rechercher un film"),
        (430, 390, "Consulter les details"),
        (850, 390, "Gerer les favoris"),
        (430, 560, "Ajouter photo de profil"),
        (850, 560, "Chercher cinemas proches"),
        (630, 710, "Dialoguer avec CineBot"),
    ]
    for x, y, text in cases:
        d.ellipse((x, y, x + 300, y + 90), fill="#F7F9FC", outline="#2E74B5", width=3)
        font = get_font(21)
        lines = wrap_text(text, font, 240)
        yy = y + 22
        for line in lines:
            bbox = d.textbbox((0, 0), line, font=font)
            d.text((x + 150 - (bbox[2] - bbox[0]) / 2, yy), line, fill="#0B2545", font=font)
            yy += 25
        arrow(d, (180, 340), (x, y + 45), color="#8A94A6", width=3)
    img.save(path)


def save_activity_diagram(path):
    img = Image.new("RGB", (1400, 1000), "#FFFFFF")
    d = ImageDraw.Draw(img)
    d.text((50, 35), "Diagramme d'activite : recherche et consultation", fill="#0B2545", font=get_font(32))
    steps = [
        ("Demarrage de l'application", "#0B2545"),
        ("Saisie texte ou recherche vocale", "#2E74B5"),
        ("Appel TMDB API", "#2E74B5"),
        ("Affichage des resultats", "#2E74B5"),
        ("Selection d'un film", "#2E74B5"),
        ("Affichage detail, casting, trailer", "#2E74B5"),
        ("Ajout aux favoris ou recherche cinema", "#E50914"),
        ("Fin du parcours", "#0B2545"),
    ]
    x, y = 420, 120
    for i, (text, color) in enumerate(steps):
        if i == 0 or i == len(steps) - 1:
            d.ellipse((x + 160, y, x + 300, y + 70), fill=color, outline=color)
            d.text((x + 177, y + 20), text, fill="#FFFFFF", font=get_font(16))
        else:
            draw_box(d, (x, y, x + 620, y + 78), text, fill="#F7F9FC", outline=color, size=22)
        if i < len(steps) - 1:
            arrow(d, (x + 310, y + 78), (x + 310, y + 118), color="#596B7A", width=4)
        y += 120
    img.save(path)


def save_class_diagram(path):
    img = Image.new("RGB", (1800, 1100), "#FFFFFF")
    d = ImageDraw.Draw(img)
    d.text((50, 35), "Diagramme de classes simplifie", fill="#0B2545", font=get_font(34))
    classes = [
        ((80, 160, 430, 380), "User", ["uid", "email", "photoUrl"], ["login()", "updateProfile()"]),
        ((560, 160, 910, 380), "MyMovieData", ["id", "title", "releaseDate", "posterPath"], ["getters"]),
        ((1040, 160, 1390, 380), "Favorite", ["userId", "movieId"], ["add()", "remove()"]),
        ((80, 520, 430, 760), "ChatMessage", ["text", "isUser", "film", "timestamp"], ["bind()"]),
        ((560, 520, 910, 760), "MovieFilm", ["tmdbId", "titre", "annee", "genre", "synopsis"], ["openDetail()"]),
        ((1040, 520, 1390, 760), "CinemaPlace", ["name", "address", "latLng", "distance"], ["formatDistance()"]),
    ]
    for xy, name, attrs, methods in classes:
        x1, y1, x2, y2 = xy
        d.rectangle(xy, fill="#FFFFFF", outline="#2E74B5", width=3)
        d.rectangle((x1, y1, x2, y1 + 55), fill="#E8EEF5", outline="#2E74B5", width=3)
        d.text((x1 + 15, y1 + 13), name, fill="#0B2545", font=get_font(24))
        yy = y1 + 75
        for a in attrs:
            d.text((x1 + 18, yy), "+ " + a, fill="#1F1B2E", font=get_font(19))
            yy += 28
        d.line((x1, yy + 5, x2, yy + 5), fill="#2E74B5", width=2)
        yy += 20
        for m in methods:
            d.text((x1 + 18, yy), "+ " + m, fill="#1F1B2E", font=get_font(19))
            yy += 28
    arrow(d, (430, 265), (560, 265))
    arrow(d, (910, 265), (1040, 265))
    arrow(d, (430, 640), (560, 640))
    arrow(d, (910, 640), (1040, 640))
    img.save(path)


def save_chatbot_diagram(path):
    img = Image.new("RGB", (1600, 900), "#FFFFFF")
    d = ImageDraw.Draw(img)
    d.text((50, 40), "Architecture du chatbot CineBot", fill="#0B2545", font=get_font(34))
    boxes = [
        ((80, 280, 420, 430), "ChatBotActivity\nAndroid Java"),
        ((590, 280, 930, 430), "API REST\nPOST /api/chat"),
        ((1100, 220, 1450, 370), "Serveur Node.js\nExpress"),
        ((1100, 500, 1450, 650), "Modele local\nfilms marocains"),
    ]
    for xy, txt in boxes:
        draw_box(d, xy, txt, fill="#F7F9FC", outline="#2E74B5", size=24)
    arrow(d, (420, 355), (590, 355), color="#596B7A")
    arrow(d, (930, 355), (1100, 300), color="#596B7A")
    arrow(d, (1275, 370), (1275, 500), color="#596B7A")
    arrow(d, (1100, 575), (930, 400), color="#596B7A")
    d.text((500, 230), "JSON: messages + userId", fill="#596B7A", font=get_font(22))
    d.text((480, 485), "JSON: text + film + timestamp", fill="#596B7A", font=get_font(22))
    img.save(path)


def save_sequence_diagram(path, title, actors, messages):
    img = Image.new("RGB", (1800, 1000), "#FFFFFF")
    d = ImageDraw.Draw(img)
    d.text((50, 35), title, fill="#0B2545", font=get_font(34))
    start_x = 120
    gap = 360
    top = 130
    bottom = 920
    xs = []
    for i, actor in enumerate(actors):
        x = start_x + i * gap
        xs.append(x + 130)
        draw_box(d, (x, top, x + 260, top + 70), actor, fill="#F7F9FC", outline="#2E74B5", size=20)
        d.line((x + 130, top + 70, x + 130, bottom), fill="#B6C2D1", width=3)
    y = 250
    for src, dst, text in messages:
        x1 = xs[src]
        x2 = xs[dst]
        arrow(d, (x1, y), (x2, y), color="#596B7A", width=4)
        label_x = min(x1, x2) + 18
        d.text((label_x, y - 34), text, fill="#0B2545", font=get_font(19))
        y += 95
    img.save(path)


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tc_pr.append(shd)


def set_cell_text(cell, text, bold=False, color=None):
    cell.text = ""
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    run = p.add_run(text)
    run.bold = bold
    run.font.name = "Calibri"
    run.font.size = Pt(10)
    if color:
        run.font.color.rgb = color
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def add_table(doc, data, widths=None, header=True):
    table = doc.add_table(rows=len(data), cols=len(data[0]))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    for i, row in enumerate(data):
        for j, val in enumerate(row):
            cell = table.cell(i, j)
            set_cell_text(cell, str(val), bold=(header and i == 0), color=NAVY if header and i == 0 else None)
            if header and i == 0:
                set_cell_shading(cell, LIGHT_BLUE)
            if widths:
                cell.width = Inches(widths[j])
    doc.add_paragraph()
    return table


def add_callout(doc, title, body, fill="F7F9FC", accent=BLUE):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    cell = table.cell(0, 0)
    set_cell_shading(cell, fill)
    cell.text = ""
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(4)
    r = p.add_run(title)
    r.bold = True
    r.font.color.rgb = accent
    r.font.size = Pt(11)
    p2 = cell.add_paragraph()
    p2.paragraph_format.space_after = Pt(0)
    p2.paragraph_format.line_spacing = 1.15
    r2 = p2.add_run(body)
    r2.font.size = Pt(10)
    doc.add_paragraph()


def add_caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(10)
    r = p.add_run(text)
    r.italic = True
    r.font.size = Pt(9)
    r.font.color.rgb = GRAY


def add_screenshot_pair(doc, left_path, left_caption, right_path, right_caption):
    table = doc.add_table(rows=2, cols=2)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    for c in range(2):
        set_cell_shading(table.cell(1, c), LIGHT_GRAY)
    paths = [left_path, right_path]
    captions = [left_caption, right_caption]
    for idx in range(2):
        cell = table.cell(0, idx)
        cell.text = ""
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        if paths[idx] and Path(paths[idx]).exists():
            p.add_run().add_picture(str(paths[idx]), width=Inches(2.15))
        else:
            run = p.add_run("Capture indisponible")
            run.italic = True
            run.font.color.rgb = GRAY
        cap_cell = table.cell(1, idx)
        set_cell_text(cap_cell, captions[idx], bold=True, color=NAVY)
    doc.add_paragraph()


def add_single_screenshot(doc, image_path, caption):
    if image_path and Path(image_path).exists():
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.add_run().add_picture(str(image_path), width=Inches(2.35))
        add_caption(doc, caption)
    else:
        add_callout(doc, caption, "Capture non disponible. Elle peut etre ajoutee ulterieurement dans cette section.", fill="FFF4F4", accent=RED)


def add_screenshot_showcase(doc, image_path, title, caption, description):
    add_heading(doc, title, 3)
    add_para(doc, description)
    if image_path and Path(image_path).exists():
        table = doc.add_table(rows=1, cols=1)
        table.alignment = WD_TABLE_ALIGNMENT.CENTER
        table.style = "Table Grid"
        cell = table.cell(0, 0)
        set_cell_shading(cell, "FFFFFF")
        cell.text = ""
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(8)
        p.paragraph_format.space_after = Pt(8)
        p.add_run().add_picture(str(image_path), width=Inches(2.65))
        add_caption(doc, caption)
    else:
        add_callout(doc, caption, "Capture non disponible.", fill="FFF4F4", accent=RED)


def add_heading(doc, text, level=1):
    p = doc.add_heading(text, level=level)
    for run in p.runs:
        run.font.name = "Calibri"
        if level == 1:
            run.font.color.rgb = BLUE
        elif level == 2:
            run.font.color.rgb = BLUE
        else:
            run.font.color.rgb = DARK_BLUE
    return p


def add_para(doc, text="", bold_start=None):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(8)
    p.paragraph_format.line_spacing = 1.25
    if bold_start and text.startswith(bold_start):
        r1 = p.add_run(bold_start)
        r1.bold = True
        p.add_run(text[len(bold_start):])
    else:
        p.add_run(text)
    return p


def add_bullets(doc, items):
    for item in items:
        p = doc.add_paragraph(style="List Bullet")
        p.paragraph_format.space_after = Pt(4)
        p.add_run(item)


def section_title_page(doc, title):
    doc.add_page_break()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(220)
    r = p.add_run(title)
    r.bold = True
    r.font.size = Pt(24)
    r.font.color.rgb = NAVY
    doc.add_page_break()


def add_page_number(paragraph):
    run = paragraph.add_run()
    fld_char1 = OxmlElement("w:fldChar")
    fld_char1.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = "PAGE"
    fld_char2 = OxmlElement("w:fldChar")
    fld_char2.set(qn("w:fldCharType"), "end")
    run._r.append(fld_char1)
    run._r.append(instr)
    run._r.append(fld_char2)


def configure_doc(doc):
    section = doc.sections[0]
    section.page_width = Inches(8.5)
    section.page_height = Inches(11)
    section.top_margin = Inches(1)
    section.bottom_margin = Inches(1)
    section.left_margin = Inches(1)
    section.right_margin = Inches(1)
    section.header_distance = Inches(0.5)
    section.footer_distance = Inches(0.5)

    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Calibri"
    normal.font.size = Pt(11)
    normal.paragraph_format.space_after = Pt(8)
    normal.paragraph_format.line_spacing = 1.25
    for name, size, color in [
        ("Title", 24, NAVY),
        ("Heading 1", 16, BLUE),
        ("Heading 2", 13, BLUE),
        ("Heading 3", 12, DARK_BLUE),
    ]:
        st = styles[name]
        st.font.name = "Calibri"
        st.font.size = Pt(size)
        st.font.color.rgb = color
        st.font.bold = True
    styles["Heading 1"].paragraph_format.space_before = Pt(16)
    styles["Heading 1"].paragraph_format.space_after = Pt(8)
    styles["Heading 2"].paragraph_format.space_before = Pt(12)
    styles["Heading 2"].paragraph_format.space_after = Pt(6)

    header = section.header.paragraphs[0]
    header.text = "MoviesApp - Rapport de projet"
    header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    header.runs[0].font.size = Pt(9)
    header.runs[0].font.color.rgb = GRAY
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    footer.add_run("Page ")
    add_page_number(footer)


def add_cover(doc):
    if LOGO_PATH.exists():
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.add_run().add_picture(str(LOGO_PATH), width=Inches(2.6))
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(24)
    r = p.add_run("Rapport de Projet")
    r.bold = True
    r.font.size = Pt(26)
    r.font.color.rgb = NAVY

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(10)
    r = p.add_run("Conception et developpement d'une application mobile Android de recherche, recommandation et geolocalisation de films")
    r.bold = True
    r.font.size = Pt(18)
    r.font.color.rgb = BLUE

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(28)
    r = p.add_run("Application : MoviesApp")
    r.bold = True
    r.font.size = Pt(20)
    r.font.color.rgb = RED

    table = doc.add_table(rows=6, cols=2)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    rows = [
        ("Etablissement", "EMSI - Ecole Marocaine des Sciences de l'Ingenieur"),
        ("Realise par", "Rim Sakine"),
        ("Filiere", "Ingenierie Informatique et Reseaux"),
        ("Encadrant pedagogique", "[Nom de l'encadrant]"),
        ("Encadrant professionnel", "[Nom de l'encadrant]"),
        ("Annee universitaire", "2025-2026"),
    ]
    for i, (k, v) in enumerate(rows):
        set_cell_text(table.cell(i, 0), k, bold=True, color=NAVY)
        set_cell_text(table.cell(i, 1), v)
        set_cell_shading(table.cell(i, 0), LIGHT_BLUE)
    doc.add_paragraph()

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(30)
    r = p.add_run("Casablanca - Mai 2026")
    r.font.size = Pt(12)
    r.font.color.rgb = GRAY
    doc.add_page_break()


def add_preliminaries(doc):
    add_heading(doc, "Dedicaces", 1)
    add_para(doc, "Je dedie ce travail a ma famille, pour son soutien permanent, sa patience et ses encouragements tout au long de mon parcours academique. Je le dedie egalement a mes enseignants et a toutes les personnes qui ont contribue, de pres ou de loin, a la realisation de ce projet.")
    doc.add_page_break()

    add_heading(doc, "Remerciements", 1)
    add_para(doc, "Je tiens a exprimer ma profonde gratitude a l'Ecole Marocaine des Sciences de l'Ingenieur pour la qualite de la formation dispensee et pour l'environnement pedagogique favorable au developpement des competences techniques et professionnelles.")
    add_para(doc, "Je remercie egalement mes encadrants pour leurs orientations, leurs remarques constructives et leur accompagnement. Mes remerciements s'adressent aussi a l'ensemble des enseignants et camarades ayant contribue a l'enrichissement de ce travail.")
    doc.add_page_break()

    add_heading(doc, "Resume", 1)
    add_para(doc, "Ce projet porte sur la conception et la realisation d'une application mobile Android intitulee MoviesApp, developpee en Java sous Android Studio. L'application permet aux utilisateurs de consulter une liste de films, rechercher des titres, visualiser les details, regarder les bandes-annonces, gerer une liste de favoris et personnaliser leur profil.")
    add_para(doc, "Le projet integre plusieurs services modernes : l'API TMDB pour l'acces aux donnees cinematographiques, Firebase Authentication et Firestore pour la gestion des utilisateurs, Supabase Storage pour le stockage des photos de profil, Google Maps et Places API pour la localisation des cinemas proches, ainsi qu'un serveur Node.js Express connecte a l'application pour fournir un chatbot de recommandation nomme CineBot. L'application prend egalement en charge la recherche vocale, la camera et le suivi GPS.")
    add_para(doc, "L'objectif principal est de proposer une experience mobile fluide, interactive et fonctionnelle, combinant consultation de contenus, intelligence conversationnelle et services geolocalises.")
    add_heading(doc, "Mots-cles", 2)
    add_para(doc, "Android, Java, TMDB, Firebase, Supabase, Google Maps, Chatbot")
    doc.add_page_break()

    add_heading(doc, "Abstract", 1)
    add_para(doc, "This project focuses on the design and implementation of an Android mobile application named MoviesApp, developed in Java using Android Studio. The application allows users to browse movies, search titles, view details, watch trailers, manage favorites and customize their profile.")
    add_para(doc, "The project integrates several modern services: TMDB API for movie data, Firebase Authentication and Firestore for user management, Supabase Storage for profile photo hosting, Google Maps and Places API for nearby cinema location, and a Node.js Express server connected to the Android application to provide a recommendation chatbot called CineBot. The application also supports voice search, camera access and GPS tracking.")
    add_para(doc, "The main objective is to provide a smooth, interactive and functional mobile experience combining content browsing, conversational assistance and location-based services.")
    add_heading(doc, "Keywords", 2)
    add_para(doc, "Android, Java, TMDB, Firebase, Supabase, Google Maps, Chatbot")
    doc.add_page_break()

    add_heading(doc, "Liste des abreviations", 1)
    add_table(doc, [
        ["Abreviation", "Signification"],
        ["API", "Application Programming Interface"],
        ["APK", "Android Package Kit"],
        ["CRUD", "Create, Read, Update, Delete"],
        ["GPS", "Global Positioning System"],
        ["HTTP", "HyperText Transfer Protocol"],
        ["JSON", "JavaScript Object Notation"],
        ["SDK", "Software Development Kit"],
        ["TMDB", "The Movie Database"],
        ["UML", "Unified Modeling Language"],
        ["UI", "User Interface"],
    ], widths=[1.5, 5.0])
    add_caption(doc, "Tableau 1 : Liste des abreviations")
    doc.add_page_break()

    add_heading(doc, "Liste des figures", 1)
    figures = [
        "Figure 1 : Architecture generale de MoviesApp",
        "Figure 2 : Diagramme de cas d'utilisation",
        "Figure 3 : Diagramme d'activite : recherche et consultation",
        "Figure 4 : Diagramme de classes simplifie",
        "Figure 5 : Diagramme de sequence - recherche de film",
        "Figure 6 : Diagramme de sequence - chatbot CineBot",
        "Figure 7 : Diagramme de sequence - photo de profil",
        "Figure 8 : Ecran d'inscription",
        "Figure 9 : Ecran de connexion",
        "Figure 10 : Ecran d'accueil et liste des films",
        "Figure 11 : Recherche de films",
        "Figure 12 : Liste des favoris",
        "Figure 13 : Detail d'un film",
        "Figure 14 : Architecture du chatbot CineBot",
    ]
    add_bullets(doc, figures)
    doc.add_page_break()

    add_heading(doc, "Liste des tableaux", 1)
    tables = [
        "Tableau 1 : Liste des abreviations",
        "Tableau 2 : Planification du projet",
        "Tableau 3 : Contraintes et mesures de maîtrise",
        "Tableau 4 : Besoins fonctionnels",
        "Tableau 5 : Besoins non fonctionnels",
        "Tableau 6 : Traçabilité entre besoins et composants",
        "Tableau 7 : Technologies utilisées",
        "Tableau 8 : Environnement de travail",
        "Tableau 9 : Tests fonctionnels",
    ]
    add_bullets(doc, tables)
    doc.add_page_break()

    add_heading(doc, "Table des matieres", 1)
    toc_items = [
        "Introduction Generale",
        "Chapitre 1 : Contexte General du Projet",
        "Chapitre 2 : Analyse et Conception",
        "Chapitre 3 : Etude Technique",
        "Chapitre 4 : Mise en oeuvre",
        "Conclusion Generale et Perspectives",
        "References Bibliographiques",
        "Annexes",
    ]
    for item in toc_items:
        p = doc.add_paragraph(style="List Number")
        p.add_run(item)
    doc.add_page_break()


def add_intro(doc):
    add_heading(doc, "Introduction Generale", 1)
    add_para(doc, "Le secteur du divertissement numerique connait une evolution importante grace a la disponibilite des plateformes de streaming, des bases de donnees cinematographiques et des services mobiles personnalises. Dans ce contexte, les utilisateurs recherchent des applications capables de leur fournir rapidement des informations fiables sur les films, des recommandations pertinentes et des services pratiques tels que la localisation de cinemas proches.")
    add_para(doc, "Le present projet s'inscrit dans cette dynamique. Il consiste a developper une application Android nommee MoviesApp, orientee vers la recherche et la consultation de films. L'application ne se limite pas a l'affichage des donnees cinematographiques : elle integre egalement des fonctionnalites modernes telles que l'authentification, les favoris, la camera, la recherche vocale, la geolocalisation et un chatbot de recommandation.")
    add_para(doc, "Le rapport est organise en quatre chapitres. Le premier chapitre presente le contexte general, la problematique, la solution proposee et la planification. Le deuxieme chapitre traite l'analyse fonctionnelle et la conception UML. Le troisieme chapitre expose l'architecture technique et les technologies utilisees. Le quatrieme chapitre decrit la mise en oeuvre, les tests et les aspects DevOps du projet.")
    doc.add_page_break()


def add_chapter1(doc):
    add_heading(doc, "Chapitre 1 : Contexte General du Projet", 1)
    add_heading(doc, "Introduction", 2)
    add_para(doc, "Ce chapitre presente le cadre general du projet MoviesApp. Il expose la problematique rencontree, la solution proposee ainsi que la methode de travail adoptee pour organiser la realisation de l'application.")

    add_heading(doc, "I. Presentation du Projet", 2)
    add_para(doc, "MoviesApp est une application mobile Android destinee aux utilisateurs souhaitant rechercher des films, consulter leurs informations detaillees et obtenir des recommandations. Elle centralise plusieurs services dans une interface unique : base de donnees de films, authentification, favoris, profil utilisateur, recherche vocale, chatbot, carte et cinemas proches.")

    add_heading(doc, "I.1 Problematique", 3)
    add_para(doc, "Les applications classiques de consultation de films offrent souvent des informations limitees ou se concentrent uniquement sur l'affichage d'un catalogue. Elles ne proposent pas toujours une experience complete combinant personnalisation, interaction vocale, recommandation conversationnelle et geolocalisation.")
    add_para(doc, "La problematique peut donc etre formulee ainsi : comment concevoir une application mobile Android permettant a un utilisateur de rechercher facilement des films, de gerer ses preferences, d'interagir avec un assistant de recommandation et de localiser les cinemas proches de sa position ?")

    add_heading(doc, "I.2 Solution Proposee", 3)
    add_para(doc, "La solution proposee consiste a developper une application Android en Java, reposant sur une architecture modulaire et connectee a plusieurs services externes. L'application exploite TMDB pour les informations cinematographiques, Firebase pour l'authentification et Firestore, Supabase pour les photos de profil, Google Maps et Places API pour les cinemas proches, ainsi qu'un backend Node.js Express pour le chatbot CineBot.")
    add_bullets(doc, [
        "Recherche de films par texte et par commande vocale.",
        "Consultation detaillee des films avec affiche, resume, note, casting et trailer.",
        "Gestion des favoris par utilisateur connecte.",
        "Profil utilisateur avec prise de photo via la camera et stockage cloud.",
        "Carte interactive affichant les cinemas proches dans un rayon de 5 km.",
        "Chatbot CineBot oriente vers la recommandation de films marocains.",
    ])

    add_heading(doc, "II. Methodologie de Travail et Planification", 2)
    add_heading(doc, "II.1 Methodologie", 3)
    add_para(doc, "La methodologie adoptee est une demarche iterative proche de l'approche agile. Le projet a ete decoupe en modules fonctionnels afin de faciliter l'analyse, le developpement, les tests et la correction progressive des anomalies.")
    add_bullets(doc, [
        "Analyse des besoins et identification des fonctionnalites prioritaires.",
        "Conception de l'architecture Android et du backend chatbot.",
        "Developpement progressif des ecrans et des services.",
        "Tests sur telephone Android reel et correction des erreurs.",
        "Integration finale des services externes et validation fonctionnelle.",
    ])

    add_heading(doc, "II.2 Diagramme de Gantt", 3)
    add_table(doc, [
        ["Phase", "Semaine 1", "Semaine 2", "Semaine 3", "Semaine 4", "Semaine 5", "Semaine 6"],
        ["Analyse et cahier des charges", "X", "X", "", "", "", ""],
        ["Conception UML", "", "X", "X", "", "", ""],
        ["Developpement Android", "", "", "X", "X", "X", ""],
        ["Backend Node.js chatbot", "", "", "", "X", "X", ""],
        ["Integration APIs et cloud", "", "", "", "X", "X", "X"],
        ["Tests et correction", "", "", "", "", "X", "X"],
        ["Redaction du rapport", "", "", "", "", "", "X"],
    ], widths=[2.1, .7, .7, .7, .7, .7, .7])
    add_caption(doc, "Tableau 2 : Planification du projet")
    add_heading(doc, "Contraintes, risques et périmètre", 2)
    add_para(doc, "La réalisation de MoviesApp a nécessité la prise en compte de plusieurs contraintes liées au développement mobile, aux services cloud et aux APIs externes. Les principales contraintes concernent la disponibilité du réseau, la validité des clés API, la gestion des permissions Android et le fonctionnement réel des services sur téléphone physique.")
    add_table(doc, [
        ["Risque", "Impact", "Mesure appliquée"],
        ["Clé Google Maps ou Places mal configurée", "Carte ou cinémas proches indisponibles", "Affichage détaillé des erreurs et configuration Google Cloud."],
        ["Service vocal indisponible", "Recherche par microphone impossible", "Vérification du service Google Voice Search et gestion de l'erreur."],
        ["Règles Supabase Storage restrictives", "Blocage de l'envoi de la photo", "Configuration RLS et stockage local de secours."],
        ["Serveur chatbot non lancé", "Chatbot inaccessible depuis Android", "Création d'une route santé et lancement local via npm."],
    ], widths=[2.1, 2.2, 2.2])
    add_caption(doc, "Tableau 3 : Contraintes et mesures de maîtrise")
    add_heading(doc, "Périmètre retenu et limites du prototype", 2)
    add_para(doc, "Le prototype couvre les fonctionnalités essentielles demandées : authentification, consultation des films, recherche vocale, favoris, profil avec photo, chatbot et carte. Certaines limites restent toutefois présentes : le chatbot fonctionne via un serveur local Node.js, la disponibilité de Places API dépend de la configuration Google Cloud, et les recommandations avancées par intelligence artificielle peuvent être améliorées dans une version future.")
    add_heading(doc, "Conclusion", 2)
    add_para(doc, "Ce chapitre a permis de definir le contexte du projet, la problematique et la solution proposee. Il a egalement presente l'organisation du travail adoptee pour assurer une progression coherente du developpement.")
    doc.add_page_break()


def add_chapter2(doc, figs):
    add_heading(doc, "Chapitre 2 : Analyse et Conception", 1)
    add_heading(doc, "Introduction", 2)
    add_para(doc, "L'analyse et la conception constituent une phase essentielle dans le cycle de developpement. Elles permettent de transformer les besoins utilisateurs en specifications fonctionnelles, puis en modeles de conception exploitables lors de l'implementation.")

    add_heading(doc, "I. Etude Fonctionnelle", 2)
    add_heading(doc, "I.1 Analyse des besoins fonctionnels", 3)
    add_table(doc, [
        ["Code", "Besoin fonctionnel", "Description"],
        ["BF01", "Authentification", "Permettre a l'utilisateur de creer un compte et de se connecter."],
        ["BF02", "Consultation des films", "Afficher une liste de films provenant de l'API TMDB."],
        ["BF03", "Recherche", "Rechercher un film par texte ou par voix."],
        ["BF04", "Details film", "Afficher le resume, l'affiche, la note, la date, le casting et le trailer."],
        ["BF05", "Favoris", "Ajouter ou supprimer un film de la liste des favoris."],
        ["BF06", "Profil", "Prendre une photo et la stocker dans Supabase Storage."],
        ["BF07", "Chatbot", "Dialoguer avec CineBot pour recevoir des recommandations."],
        ["BF08", "Carte", "Afficher les cinemas proches a l'aide de Google Maps et Places API."],
    ], widths=[.8, 1.8, 3.9])
    add_caption(doc, "Tableau 4 : Besoins fonctionnels")

    add_heading(doc, "I.2 Besoins non fonctionnels", 3)
    add_table(doc, [
        ["Critere", "Description"],
        ["Performance", "Les donnees doivent se charger rapidement et l'interface doit rester fluide."],
        ["Securite", "Les acces utilisateur doivent etre geres par Firebase Authentication et les cles doivent etre protegees par restriction."],
        ["Ergonomie", "L'interface doit etre claire, lisible et adaptee aux ecrans mobiles."],
        ["Maintenabilite", "Le code doit etre organise par activite, adaptateur et modele."],
        ["Disponibilite", "L'application doit gerer les erreurs reseau et informer l'utilisateur."],
        ["Compatibilite", "L'application doit fonctionner sur un telephone Android reel avec les permissions necessaires."],
    ], widths=[1.6, 4.9])
    add_caption(doc, "Tableau 5 : Besoins non fonctionnels")

    add_heading(doc, "II. Etude Conceptuelle", 2)
    add_heading(doc, "II.1 Regles de gestion", 3)
    add_bullets(doc, [
        "Un utilisateur doit etre authentifie pour acceder a son profil et a ses favoris.",
        "Chaque utilisateur possede une liste de films favoris independante.",
        "La photo de profil est associee a l'identifiant unique de l'utilisateur.",
        "La recherche vocale necessite la permission RECORD_AUDIO et un service de reconnaissance disponible.",
        "La localisation des cinemas necessite l'autorisation GPS et une cle Google Cloud valide.",
        "CineBot recommande uniquement des films marocains et retourne, lorsque possible, les informations du film recommande.",
    ])

    add_heading(doc, "II.2 Diagrammes UML", 3)
    doc.add_picture(str(figs["use_case"]), width=Inches(6.4))
    add_caption(doc, "Figure 2 : Diagramme de cas d'utilisation")
    add_para(doc, "Le diagramme de cas d'utilisation met en evidence les principales interactions entre l'utilisateur et le systeme MoviesApp : authentification, recherche, consultation, favoris, profil, chatbot et carte.")

    doc.add_picture(str(figs["activity"]), width=Inches(5.9))
    add_caption(doc, "Figure 3 : Diagramme d'activite : recherche et consultation")
    add_para(doc, "Le diagramme d'activite illustre le parcours principal de l'utilisateur depuis le demarrage de l'application jusqu'a la consultation d'un film et l'execution d'actions complementaires.")

    doc.add_picture(str(figs["class"]), width=Inches(6.4))
    add_caption(doc, "Figure 4 : Diagramme de classes simplifie")
    add_para(doc, "Le diagramme de classes represente les entites majeures manipulees par l'application, notamment les films, les messages du chatbot, les cinemas proches et les informations utilisateur.")

    add_heading(doc, "Traçabilité entre besoins et composants", 2)
    add_table(doc, [
        ["Besoin", "Composant Android / Backend", "Service associé"],
        ["Authentification", "LoginActivity, RegisterActivity", "Firebase Auth"],
        ["Recherche et affichage films", "MainActivity, MyMovieAdapter", "TMDB API"],
        ["Détails et trailer", "MovieDetailActivity, VideoPlayer", "TMDB API, YouTube"],
        ["Favoris", "FavoritesActivity, MovieDetailActivity", "Cloud Firestore"],
        ["Photo profil", "ProfileActivity", "Camera, Supabase Storage"],
        ["Recherche vocale", "MainActivity", "SpeechRecognizer"],
        ["Cinémas proches", "MapActivity", "FusedLocationProvider, Google Places"],
        ["Chatbot", "ChatBotActivity, serveur Express", "Node.js API"],
    ], widths=[2.0, 2.7, 1.8])
    add_caption(doc, "Tableau 6 : Traçabilité entre besoins et composants")

    add_heading(doc, "II.4 Diagrammes de séquence", 3)
    doc.add_picture(str(figs["sequence_search"]), width=Inches(6.4))
    add_caption(doc, "Figure 5 : Diagramme de séquence - recherche de film")
    doc.add_picture(str(figs["sequence_chatbot"]), width=Inches(6.4))
    add_caption(doc, "Figure 6 : Diagramme de séquence - chatbot CineBot")
    doc.add_picture(str(figs["sequence_profile"]), width=Inches(6.4))
    add_caption(doc, "Figure 7 : Diagramme de séquence - photo de profil")

    add_heading(doc, "Conclusion", 2)
    add_para(doc, "Ce chapitre a permis de formaliser les besoins et de produire une conception claire du systeme. Ces elements constituent la base technique et fonctionnelle de la phase d'implementation.")
    doc.add_page_break()


def add_chapter3(doc, figs):
    add_heading(doc, "Chapitre 3 : Etude Technique", 1)
    add_heading(doc, "Introduction", 2)
    add_para(doc, "L'etude technique presente les choix technologiques et l'architecture globale adoptes pour realiser MoviesApp. Ces choix ont ete effectues en tenant compte de la compatibilite Android, de la simplicite d'integration et de la disponibilite des services cloud.")

    add_heading(doc, "I. Architecture du projet", 2)
    doc.add_picture(str(figs["architecture"]), width=Inches(6.4))
    add_caption(doc, "Figure 1 : Architecture generale de MoviesApp")
    add_para(doc, "L'application Android joue le role de client principal. Elle communique avec plusieurs services externes via HTTP et SDK : TMDB pour les films, Firebase pour les utilisateurs, Supabase pour le stockage d'images, Google Maps/Places pour la cartographie et Node.js pour le chatbot.")

    add_heading(doc, "II. Technologies utilisees", 2)
    add_table(doc, [
        ["Technologie", "Role dans le projet"],
        ["Android Studio", "Environnement principal de developpement mobile."],
        ["Java", "Langage utilise pour les activites, adaptateurs et modeles Android."],
        ["XML", "Creation des interfaces utilisateur Android."],
        ["TMDB API", "Recuperation des films, affiches, details, casting et trailers."],
        ["Firebase Auth", "Authentification des utilisateurs."],
        ["Cloud Firestore", "Stockage des favoris et informations utilisateur."],
        ["Supabase Storage", "Stockage des photos de profil."],
        ["Google Maps SDK", "Affichage de la carte dans l'application."],
        ["Google Places API", "Recherche des cinemas proches."],
        ["Node.js / Express", "Backend du chatbot CineBot."],
        ["Volley", "Gestion des requetes HTTP cote Android."],
        ["Glide", "Chargement et affichage des images."],
    ], widths=[2.0, 4.5])
    add_caption(doc, "Tableau 7 : Technologies utilisees")

    add_heading(doc, "III. Environnement de travail", 2)
    add_table(doc, [
        ["Element", "Description"],
        ["Systeme d'exploitation", "Windows"],
        ["IDE mobile", "Android Studio"],
        ["IDE backend", "Visual Studio Code"],
        ["Langage mobile", "Java"],
        ["Langage backend", "JavaScript"],
        ["Gestionnaire backend", "npm"],
        ["Telephone de test", "Samsung Android reel"],
        ["Outils de debug", "Logcat, ADB, Gradle"],
    ], widths=[2.0, 4.5])
    add_caption(doc, "Tableau 8 : Environnement de travail")

    doc.add_picture(str(figs["chatbot"]), width=Inches(6.4))
    add_caption(doc, "Figure 14 : Architecture du chatbot CineBot")
    add_para(doc, "Le chatbot repose sur une communication REST entre ChatBotActivity et un serveur Express. L'application envoie l'historique de conversation et le serveur retourne une reponse textuelle ainsi qu'un objet film lorsque la recommandation est disponible.")

    add_heading(doc, "Conclusion", 2)
    add_para(doc, "Ce chapitre a detaille l'architecture technique et les technologies retenues. L'ensemble de ces outils permet de construire une application mobile connectee, interactive et extensible.")
    doc.add_page_break()


def add_chapter4(doc):
    add_heading(doc, "Chapitre 4 : Mise en oeuvre", 1)
    add_heading(doc, "Introduction", 2)
    add_para(doc, "La mise en oeuvre correspond a la transformation de la conception en une application fonctionnelle. Cette partie presente les principaux modules developpes, les tests effectues et les elements lies a l'integration continue du projet.")

    add_heading(doc, "I. Réalisation des écrans principaux", 2)
    screenshots = find_screenshots()
    add_para(doc, "Les captures suivantes présentent les principaux écrans développés dans l'application MoviesApp. Elles montrent le parcours utilisateur depuis l'inscription jusqu'à la consultation, la recherche et la gestion des favoris.")
    add_screenshot_pair(doc, screenshots.get("register"), "Figure 8 : Écran d'inscription", screenshots.get("login"), "Figure 9 : Écran de connexion")
    add_screenshot_pair(doc, screenshots.get("home"), "Figure 10 : Écran d'accueil et liste des films", screenshots.get("search"), "Figure 11 : Recherche de films")
    add_screenshot_pair(doc, screenshots.get("favorites"), "Figure 12 : Liste des favoris", screenshots.get("detail"), "Figure 13 : Détail d'un film")

    add_heading(doc, "II. Réalisation fonctionnelle", 2)
    add_heading(doc, "I.1 Module d'authentification", 3)
    add_para(doc, "Le module d'authentification repose sur Firebase Authentication. Il permet l'inscription et la connexion des utilisateurs a travers LoginActivity et RegisterActivity. Chaque utilisateur possede un identifiant unique utilise pour associer ses favoris et sa photo de profil.")

    add_heading(doc, "I.2 Module principal et recherche de films", 3)
    add_para(doc, "MainActivity constitue l'ecran principal de l'application. Il affiche les films dans un RecyclerView et permet le filtrage par categories. La recherche est disponible par saisie textuelle et par reconnaissance vocale via SpeechRecognizer.")

    add_heading(doc, "I.3 Module details film", 3)
    add_para(doc, "MovieDetailActivity presente les informations detaillees d'un film : affiche, titre, resume, note, annee, casting, films recommandes et bande-annonce. Elle integre egalement une carte permettant de localiser des cinemas proches.")

    add_heading(doc, "I.4 Module profil et camera", 3)
    add_para(doc, "ProfileActivity permet a l'utilisateur de prendre une photo via la camera du telephone. L'image est ensuite envoyee vers Supabase Storage, puis l'URL publique est sauvegardee localement et synchronisee avec Firestore lorsque la connexion est disponible.")

    add_heading(doc, "I.5 Module carte et cinemas proches", 3)
    add_para(doc, "MapActivity affiche une carte plein ecran et utilise FusedLocationProviderClient pour recuperer la position de l'utilisateur. L'application interroge ensuite Places API afin d'afficher les cinemas proches dans un rayon de 5 km sous forme de marqueurs.")

    add_heading(doc, "I.6 Module chatbot CineBot", 3)
    add_para(doc, "ChatBotActivity communique avec un serveur Node.js Express. L'utilisateur envoie un message, le serveur analyse la demande et retourne une recommandation de film marocain. Si un film est identifie, l'application affiche une carte cliquable permettant d'ouvrir MovieDetailActivity.")

    add_heading(doc, "III. DevOps, tests et vérification", 2)
    add_para(doc, "Le projet a ete teste directement sur un telephone Android reel afin de valider les fonctionnalites necessitant des ressources materielles telles que le GPS, le microphone et la camera. Le debogage a ete realise avec Logcat, ADB et les messages d'erreur affiches dans l'application.")
    add_table(doc, [
        ["Test", "Resultat attendu", "Etat"],
        ["Connexion utilisateur", "Authentification reussie avec Firebase", "Valide"],
        ["Recherche film", "Affichage des films correspondant au mot cle", "Valide"],
        ["Recherche vocale", "Remplissage automatique du champ de recherche", "Valide avec service vocal disponible"],
        ["Photo profil", "Capture et upload vers Supabase", "Valide apres configuration RLS"],
        ["Chatbot", "Reponse du serveur Node.js et affichage des messages", "Valide si serveur lance"],
        ["Carte", "Affichage de la carte et des cinemas proches", "Depend de la cle Google Cloud"],
    ], widths=[1.8, 3.4, 1.3])
    add_caption(doc, "Tableau 9 : Tests fonctionnels")
    add_heading(doc, "Critères de validation pour la soutenance", 2)
    add_bullets(doc, [
        "L'utilisateur peut créer un compte, se connecter et accéder à l'application.",
        "La liste des films est chargée dynamiquement depuis TMDB.",
        "La recherche texte et la recherche vocale remplissent correctement les résultats.",
        "Le détail d'un film affiche les informations principales et les recommandations.",
        "Les favoris sont sauvegardés par utilisateur connecté.",
        "La photo de profil est capturée et envoyée vers Supabase Storage.",
        "La carte affiche la position et interroge Google Places pour les cinémas proches.",
        "Le chatbot répond depuis le serveur Node.js et propose des films marocains.",
    ])

    add_heading(doc, "Améliorations proposées", 2)
    add_bullets(doc, [
        "Déployer le serveur chatbot sur une plateforme cloud publique.",
        "Ajouter une recommandation basée sur l'historique et les favoris.",
        "Mettre en place un mode hors ligne pour les derniers films consultés.",
        "Ajouter des notifications pour les sorties de films.",
        "Améliorer la sécurité des clés API avec des restrictions adaptées.",
    ])

    add_heading(doc, "Conclusion", 2)
    add_para(doc, "La mise en oeuvre a permis d'obtenir une application Android riche en fonctionnalites et connectee a plusieurs services externes. Les tests ont montre l'importance de la bonne configuration des permissions, des regles cloud et des cles API.")
    doc.add_page_break()


def find_screenshots():
    downloads = Path(r"C:\Users\EL KHATIB\Downloads")
    data = {
        "register": downloads / "register.jpeg",
        "login": downloads / "login.jpeg",
        "home": downloads / "WhatsApp Image 2026-05-20 at 01.22.04.jpeg",
        "search": downloads / "recherche.jpeg",
        "favorites": downloads / "WhatsApp Image 2026-05-20 at 01.22.04 (3).jpeg",
        "detail": downloads / "WhatsApp Image 2026-05-20 at 01.22.04 (4).jpeg",
        "chatbot": downloads / "WhatsApp Image 2026-05-20 at 01.22.04 (2).jpeg",
        "map": downloads / "WhatsApp Image 2026-05-20 at 01.22.04 (1).jpeg",
    }
    if not data["home"].exists():
        data["home"] = downloads / "Acceuil.jpeg"
    if not data["favorites"].exists():
        data["favorites"] = downloads / "favoris.jpeg"
    if not data["detail"].exists():
        for p in downloads.glob("*.jpeg"):
            if "tail" in p.name.lower() or "dét" in p.name.lower() or "det" in p.name.lower():
                data["detail"] = p
                break
    return data


def add_extended_professional_sections(doc, figs):
    screenshots = find_screenshots()

    doc.add_page_break()
    add_heading(doc, "Dossier de réalisation détaillé", 1)
    add_callout(
        doc,
        "Objectif de cette partie",
        "Cette section complète le chapitre de mise en œuvre par une description plus opérationnelle des écrans, "
        "des flux, des services et des validations effectuées. Elle permet de rapprocher le rapport du format "
        "attendu pour une soutenance PFA professionnelle.",
        fill="E8EEF5",
        accent=NAVY,
    )
    add_heading(doc, "1. Parcours utilisateur principal", 2)
    add_para(doc, "Le parcours utilisateur commence par l'authentification, puis se poursuit avec la consultation des films. Une fois connecté, l'utilisateur peut rechercher un film, consulter ses détails, l'ajouter aux favoris, dialoguer avec CineBot ou accéder à la carte des cinémas proches.")
    add_table(doc, [
        ["Étape", "Action utilisateur", "Réponse de l'application"],
        ["1", "Ouverture de l'application", "Affichage de l'écran de connexion ou de l'accueil selon l'état de session."],
        ["2", "Connexion ou inscription", "Authentification via Firebase et redirection vers MainActivity."],
        ["3", "Recherche de film", "Interrogation de TMDB et affichage des résultats dans RecyclerView."],
        ["4", "Consultation d'un film", "Affichage des détails, casting, note, résumé et trailer."],
        ["5", "Actions avancées", "Favoris, recherche vocale, photo de profil, chatbot ou carte."],
    ], widths=[.7, 2.2, 3.6])
    add_caption(doc, "Tableau 10 : Parcours utilisateur principal")

    add_heading(doc, "2. Galerie des captures d'écran de l'application", 2)
    add_para(doc, "Les captures suivantes montrent les écrans réellement réalisés dans le prototype MoviesApp. Afin de conserver une lecture professionnelle, chaque capture est présentée séparément avec une courte analyse fonctionnelle.")
    add_screenshot_showcase(
        doc,
        screenshots.get("home"),
        "2.1 Écran d'accueil",
        "Figure 15 : Écran d'accueil de MoviesApp",
        "L'écran d'accueil regroupe la recherche, les catégories, l'accès au profil, le bouton des cinémas proches et la liste principale des films. Le design sombre met en avant les affiches et utilise le rouge comme couleur d'action principale."
    )
    doc.add_page_break()
    add_screenshot_showcase(
        doc,
        screenshots.get("detail"),
        "2.2 Écran détail du film",
        "Figure 16 : Écran détail du film",
        "L'écran de détail présente les informations essentielles du film : affiche, titre, note, année, genres, résumé, casting, bouton trailer et accès à la partie localisation. Cette page centralise les informations nécessaires avant la décision de visionnage."
    )
    doc.add_page_break()
    add_screenshot_showcase(
        doc,
        screenshots.get("favorites"),
        "2.3 Écran des favoris",
        "Figure 17 : Liste des films favoris",
        "La page des favoris permet à l'utilisateur de retrouver les films sauvegardés. Chaque élément conserve la même structure visuelle que la liste principale afin d'assurer la cohérence de navigation."
    )
    doc.add_page_break()
    add_screenshot_showcase(
        doc,
        screenshots.get("chatbot"),
        "2.4 Interface du chatbot CineBot",
        "Figure 18 : Interface du chatbot CineBot",
        "CineBot est présenté comme un assistant conversationnel spécialisé dans les recommandations de films marocains. L'interface contient une zone de discussion, un message d'accueil et un champ de saisie."
    )
    doc.add_page_break()
    add_screenshot_showcase(
        doc,
        screenshots.get("map"),
        "2.5 Carte des cinémas proches",
        "Figure 19 : Carte des cinémas proches",
        "La carte affiche la position de l'utilisateur ainsi que les cinémas détectés dans un rayon de cinq kilomètres. Les marqueurs permettent une lecture rapide des résultats géolocalisés."
    )
    doc.add_page_break()
    add_screenshot_showcase(
        doc,
        screenshots.get("login"),
        "2.6 Écran de connexion",
        "Figure 20 : Écran de connexion",
        "L'écran de connexion donne accès à l'espace utilisateur. Il respecte la charte graphique sombre de l'application et prépare l'utilisateur à accéder aux fonctionnalités personnalisées."
    )
    add_screenshot_showcase(
        doc,
        screenshots.get("register"),
        "2.7 Écran d'inscription",
        "Figure 21 : Écran d'inscription",
        "L'écran d'inscription permet la création d'un nouveau compte. Il constitue le point d'entrée pour les utilisateurs qui souhaitent bénéficier des favoris et du profil personnalisé."
    )
    add_callout(
        doc,
        "Remarque sur les captures",
        "Le téléphone n'étant pas toujours visible par ADB, les captures disponibles localement ont été intégrées. "
        "De nouvelles captures peuvent être ajoutées facilement en remplaçant les images de cette section.",
        fill="F7F9FC",
        accent=BLUE,
    )

    doc.add_page_break()
    add_heading(doc, "3. Détail des modules développés", 2)
    add_heading(doc, "3.1 Authentification Firebase", 3)
    add_para(doc, "Le module d'authentification repose sur Firebase Authentication. Il permet de sécuriser l'accès à l'application, de distinguer les utilisateurs et d'associer les données personnelles, notamment les favoris et la photo de profil, à un compte précis.")
    add_bullets(doc, [
        "Création de compte par adresse e-mail et mot de passe.",
        "Connexion sécurisée par Firebase Authentication.",
        "Récupération de l'utilisateur courant via FirebaseAuth.",
        "Utilisation de l'UID pour lier les favoris et les informations de profil.",
    ])

    add_heading(doc, "3.2 Consultation et recherche de films", 3)
    add_para(doc, "La consultation des films s'appuie sur l'API TMDB. Les données sont récupérées sous forme JSON, transformées en objets Java, puis affichées dans un RecyclerView à l'aide d'un adaptateur personnalisé. L'utilisateur peut rechercher un film par saisie textuelle ou par recherche vocale.")
    add_table(doc, [
        ["Élément", "Description technique"],
        ["MainActivity", "Écran principal, gestion de la liste des films et de la recherche."],
        ["MyMovieAdapter", "Adaptateur RecyclerView responsable de l'affichage des cartes films."],
        ["Volley", "Bibliothèque utilisée pour les appels HTTP vers TMDB."],
        ["Glide", "Chargement optimisé des affiches depuis les URLs TMDB."],
        ["SpeechRecognizer", "Service Android utilisé pour convertir la voix en texte."],
    ], widths=[1.8, 4.7])
    add_caption(doc, "Tableau 11 : Composants du module films")

    add_heading(doc, "3.3 Profil, caméra et stockage Supabase", 3)
    add_para(doc, "Le profil utilisateur permet de personnaliser l'expérience grâce à une photo prise directement avec la caméra. L'image est envoyée vers Supabase Storage. L'URL publique est ensuite conservée localement et peut être synchronisée avec Firestore.")
    add_callout(
        doc,
        "Point technique important",
        "La difficulté principale a été la configuration des règles RLS de Supabase Storage. Sans politique d'insertion adaptée, Supabase refuse l'envoi de l'image avec une erreur 403.",
        fill="FFF4F4",
        accent=RED,
    )

    add_heading(doc, "3.4 Carte, GPS et Google Places", 3)
    add_para(doc, "La carte s'appuie sur Google Maps SDK pour l'affichage et sur FusedLocationProviderClient pour récupérer la position. La recherche des cinémas proches utilise Places API (New) avec l'endpoint places:searchNearby.")
    add_table(doc, [
        ["Fonction", "Implémentation"],
        ["Affichage de carte", "SupportMapFragment et GoogleMap."],
        ["Localisation", "FusedLocationProviderClient avec permissions runtime."],
        ["Recherche cinémas", "Places API (New), type movie_theater, rayon 5 km."],
        ["Marqueurs", "Markers rouges affichant nom, adresse et distance."],
        ["Fallback", "Position Casablanca utilisée si le GPS est indisponible."],
    ], widths=[1.8, 4.7])
    add_caption(doc, "Tableau 12 : Implémentation du module cartographie")

    doc.add_page_break()
    add_heading(doc, "4. Architecture backend du chatbot", 2)
    doc.add_picture(str(figs["chatbot"]), width=Inches(6.4))
    add_caption(doc, "Figure 23 : Architecture détaillée de CineBot")
    add_para(doc, "CineBot est relié à l'application Android par un serveur Node.js Express. L'objectif est de séparer l'interface mobile de la logique de recommandation. Android envoie l'historique de conversation au serveur, qui renvoie une réponse textuelle et éventuellement une fiche film.")
    add_table(doc, [
        ["Fichier", "Rôle"],
        ["server.js", "Initialisation Express, CORS, body-parser et routes principales."],
        ["routes/chat.js", "Réception des messages Android et construction de la réponse JSON."],
        ["services/cinebotModel.js", "Logique locale de recommandation des films marocains."],
        [".env", "Variables d'environnement du serveur."],
        ["README.md", "Instructions d'installation et de test."],
    ], widths=[2.0, 4.5])
    add_caption(doc, "Tableau 13 : Structure du serveur chatbot")
    add_para(doc, "Le chatbot est conçu comme un assistant spécialisé. Il ne remplace pas une intelligence artificielle générale, mais il répond à un besoin précis : recommander des films marocains selon les préférences exprimées par l'utilisateur.")

    doc.add_page_break()
    add_heading(doc, "5. Sécurité, permissions et configuration", 2)
    add_para(doc, "L'application utilise plusieurs permissions sensibles. Leur gestion est importante pour respecter les règles Android récentes et éviter les erreurs au lancement des fonctionnalités.")
    add_table(doc, [
        ["Permission", "Utilisation", "Gestion"],
        ["INTERNET", "Appels TMDB, Supabase, Google Places et chatbot", "Déclarée dans AndroidManifest.xml."],
        ["CAMERA", "Prise de photo de profil", "Demandée au runtime via ActivityResultLauncher."],
        ["RECORD_AUDIO", "Recherche vocale", "Demandée au runtime avant SpeechRecognizer."],
        ["ACCESS_FINE_LOCATION", "Position GPS précise", "Demandée au runtime pour Maps."],
        ["ACCESS_COARSE_LOCATION", "Position approximative", "Demandée avec la localisation fine."],
        ["READ_MEDIA_IMAGES", "Accès images selon version Android", "Déclarée pour compatibilité Android récent."],
    ], widths=[1.7, 2.4, 2.4])
    add_caption(doc, "Tableau 14 : Permissions Android utilisées")
    add_callout(
        doc,
        "Sécurité des clés",
        "Les clés Google Maps et Places doivent être restreintes dans Google Cloud par package Android et empreinte SHA-1. Les clés sensibles côté serveur ne doivent jamais être intégrées dans le code Android.",
        fill="FFF8E8",
        accent=RGBColor(122, 90, 0),
    )

    add_heading(doc, "6. Scénarios de test détaillés", 2)
    add_table(doc, [
        ["ID", "Scénario", "Précondition", "Résultat attendu"],
        ["T01", "Inscription", "E-mail non utilisé", "Compte créé et utilisateur redirigé."],
        ["T02", "Connexion", "Compte existant", "Ouverture de l'écran principal."],
        ["T03", "Recherche textuelle", "Connexion Internet active", "Films correspondant au mot-clé affichés."],
        ["T04", "Recherche vocale", "Micro autorisé", "Texte reconnu placé dans le champ de recherche."],
        ["T05", "Ajout favori", "Utilisateur connecté", "Film ajouté dans Firestore."],
        ["T06", "Photo profil", "Caméra autorisée", "Image affichée et envoyée vers Supabase."],
        ["T07", "Chatbot", "Serveur Node.js lancé", "Réponse CineBot affichée."],
        ["T08", "Carte", "GPS et clé Google valides", "Cinémas proches affichés par marqueurs."],
    ], widths=[.6, 1.8, 2.0, 2.1])
    add_caption(doc, "Tableau 15 : Scénarios de test détaillés")

    doc.add_page_break()
    add_heading(doc, "7. Problèmes rencontrés et solutions", 2)
    add_para(doc, "Durant la réalisation, plusieurs problèmes techniques ont été rencontrés. Leur résolution a permis de stabiliser progressivement l'application et d'améliorer la qualité du prototype.")
    add_table(doc, [
        ["Problème", "Cause", "Solution"],
        ["Application non installée", "ADB non autorisé ou téléphone hors ligne", "Réautoriser le débogage USB et relancer l'installation."],
        ["Micro indisponible", "Service Google Voice Search absent ou désactivé", "Vérifier SpeechRecognizer et service Google."],
        ["Photo bloquée", "Règles Supabase RLS restrictives", "Créer des politiques Storage adaptées."],
        ["Places API refusée", "API non activée ou clé restreinte", "Activer Places API (New) et corriger les restrictions."],
        ["Chatbot inaccessible", "Serveur Node.js non lancé ou IP incorrecte", "Lancer npm run dev et utiliser l'adresse IP du PC."],
    ], widths=[2.0, 2.3, 2.2])
    add_caption(doc, "Tableau 16 : Problèmes rencontrés et solutions")

    add_heading(doc, "8. Guide rapide d'utilisation", 2)
    add_bullets(doc, [
        "Lancer l'application et créer un compte utilisateur.",
        "Se connecter avec l'adresse e-mail et le mot de passe.",
        "Parcourir la liste des films ou utiliser la barre de recherche.",
        "Appuyer sur le microphone pour effectuer une recherche vocale.",
        "Ouvrir un film pour consulter les détails et le trailer.",
        "Ajouter le film aux favoris si l'utilisateur souhaite le conserver.",
        "Ouvrir le profil pour prendre une photo et la sauvegarder.",
        "Utiliser le bouton CineBot pour demander une recommandation.",
        "Utiliser le bouton Cinémas proches pour afficher la carte.",
    ])

    doc.add_page_break()
    add_heading(doc, "9. Bilan technique", 2)
    add_para(doc, "Le projet MoviesApp représente une application mobile complète combinant plusieurs familles de fonctionnalités : interfaces Android, communication HTTP, services cloud, stockage d'images, géolocalisation, reconnaissance vocale et backend Node.js. Cette diversité renforce la valeur technique du projet et montre une maîtrise progressive de l'écosystème Android moderne.")
    add_para(doc, "La séparation entre l'application mobile et le backend chatbot constitue également un choix pertinent, car elle facilite l'évolution du chatbot indépendamment du client Android. De plus, l'utilisation de services spécialisés comme Firebase, Supabase, TMDB et Google Maps permet de construire un prototype réaliste et proche des standards actuels.")


def add_conclusion_refs_annexes(doc):
    add_heading(doc, "Conclusion Generale et Perspectives", 1)
    add_para(doc, "Le projet MoviesApp a permis de mettre en pratique plusieurs competences de developpement mobile, de conception logicielle et d'integration de services cloud. L'application obtenue combine recherche de films, authentification, favoris, profil utilisateur, photo de profil, reconnaissance vocale, geolocalisation, carte interactive et chatbot de recommandation.")
    add_para(doc, "Sur le plan technique, le projet a mis en evidence l'importance d'une architecture claire, de la gestion des permissions Android et de la configuration correcte des services externes. Les difficultes rencontrees, notamment celles liees aux cles Google Cloud, a Supabase Storage et aux services vocaux Android, ont ete traitees progressivement par analyse des messages d'erreur et tests sur telephone reel.")
    add_para(doc, "Comme perspectives d'amelioration, il serait possible d'ajouter un systeme de notation des films par les utilisateurs, une recommandation basee sur l'historique, un mode hors ligne partiel, des notifications, une interface administrateur et un deploiement cloud permanent du serveur chatbot.")
    doc.add_page_break()

    add_heading(doc, "References Bibliographiques", 1)
    refs = [
        "EMSI - Ecole Marocaine des Sciences de l'Ingenieur, site officiel : https://emsi.ma/",
        "Android Developers, documentation officielle Android : https://developer.android.com/",
        "Firebase Documentation : https://firebase.google.com/docs",
        "Supabase Documentation : https://supabase.com/docs",
        "Google Maps Platform Documentation : https://developers.google.com/maps",
        "Google Places API Documentation : https://developers.google.com/maps/documentation/places",
        "The Movie Database API Documentation : https://developer.themoviedb.org/docs",
        "Express.js Documentation : https://expressjs.com/",
        "Volley Library Documentation : https://google.github.io/volley/",
        "Glide Documentation : https://bumptech.github.io/glide/",
    ]
    for ref in refs:
        p = doc.add_paragraph(style="List Number")
        p.add_run(ref)
    doc.add_page_break()

    add_heading(doc, "Annexes", 1)
    add_heading(doc, "Annexe A : Structure simplifiee du projet Android", 2)
    add_para(doc, "Le projet Android est organise autour du package com.example.moviapp_rimsakine. Les principales classes sont MainActivity, MovieDetailActivity, ProfileActivity, MapActivity, ChatBotActivity, LoginActivity, RegisterActivity, FavoritesActivity et les adaptateurs associes.")
    add_heading(doc, "Annexe B : Structure du serveur chatbot", 2)
    add_para(doc, "Le serveur MoviesApp-Chatbot-Server contient server.js, routes/chat.js, services/cinebotModel.js, package.json, .env.example et README.md. Il expose les routes /api/health et /api/chat.")
    add_heading(doc, "Annexe C : Permissions Android utilisees", 2)
    add_bullets(doc, [
        "INTERNET pour les appels reseau.",
        "CAMERA pour la photo de profil.",
        "RECORD_AUDIO pour la recherche vocale.",
        "ACCESS_FINE_LOCATION et ACCESS_COARSE_LOCATION pour la geolocalisation.",
        "READ_MEDIA_IMAGES pour l'acces aux images selon la version Android.",
    ])


def apply_french_accents(doc):
    replacements = {
        "Rapport de Projet": "Rapport de Projet",
        "Conception et developpement": "Conception et développement",
        "geolocalisation": "géolocalisation",
        "Etablissement": "Établissement",
        "Ingenieur": "Ingénieur",
        "Ingenierie": "Ingénierie",
        "Realise": "Réalisé",
        "realise": "réalisé",
        "Filiere": "Filière",
        "Encadrant pedagogique": "Encadrant pédagogique",
        "Annee universitaire": "Année universitaire",
        "Dedie": "Dédié",
        "Dedicaces": "Dédicaces",
        "dedie": "dédie",
        "a ma famille": "à ma famille",
        "a mes": "à mes",
        "a toutes": "à toutes",
        "a exprimer": "à exprimer",
        "a l'Ecole": "à l'École",
        "Remerciements": "Remerciements",
        "qualite": "qualité",
        "dispensee": "dispensée",
        "developpement": "développement",
        "developpee": "développée",
        "developpes": "développés",
        "fonctionnalites": "fonctionnalités",
        "utilisateurs": "utilisateurs",
        "donnees": "données",
        "cinematographiques": "cinématographiques",
        "recommandations": "recommandations",
        "connectee": "connectée",
        "connectes": "connectés",
        "recherche vocale": "recherche vocale",
        "camera": "caméra",
        "geolocalises": "géolocalisés",
        "experience": "expérience",
        "fluide": "fluide",
        "interactif": "interactif",
        "Resume": "Résumé",
        "Mots-cles": "Mots-clés",
        "abreviations": "abréviations",
        "Table des matieres": "Table des matières",
        "Introduction Generale": "Introduction Générale",
        "Conclusion Generale": "Conclusion Générale",
        "Contexte General": "Contexte Général",
        "Presentation": "Présentation",
        "presentation": "présentation",
        "Etude": "Étude",
        "Methodologie": "Méthodologie",
        "Regles": "Règles",
        "regles": "règles",
        "Mise en oeuvre": "Mise en œuvre",
        "References Bibliographiques": "Références Bibliographiques",
        "preliminaires": "préliminaires",
        "debut": "début",
        "utilisee": "utilisée",
        "utilisees": "utilisées",
        "integre": "intègre",
        "integration": "intégration",
        "specifications": "spécifications",
        "necessaire": "nécessaire",
        "necessitent": "nécessitent",
        "methode": "méthode",
        "methodologie": "méthodologie",
        "Problematique": "Problématique",
        "problematiques": "problématiques",
        "proposee": "proposée",
        "Proposee": "Proposée",
        "adoptee": "adoptée",
        "activites": "activités",
        "presente": "présente",
        "presentees": "présentées",
        "presentee": "présentée",
        "element": "élément",
        "Element": "Élément",
        "ecran": "écran",
        "ecrans": "écrans",
        "affiche": "affiche",
        "afficher": "afficher",
        "details": "détails",
        "Details": "Détails",
        "gerer": "gérer",
        "Gerer": "Gérer",
        "associe": "associé",
        "associee": "associée",
        "requete": "requête",
        "requetes": "requêtes",
        "cote": "côté",
        "Systeme": "Système",
        "systeme": "système",
        "securite": "sécurité",
        "Securite": "Sécurité",
        "rapidite": "rapidité",
        "disponibilite": "disponibilité",
        "Disponibilite": "Disponibilité",
        "compatibilite": "compatibilité",
        "Compatibilite": "Compatibilité",
        "ergonomie": "ergonomie",
        "utilisateur": "utilisateur",
        "identifiant": "identifiant",
        "autorisations": "autorisations",
        "reseau": "réseau",
        "Cinemas": "Cinémas",
        "cinemas": "cinémas",
        "trouve": "trouvé",
        "trouves": "trouvés",
        "rayon": "rayon",
        "Architecture generale": "Architecture générale",
        "Diagramme d'activite": "Diagramme d'activité",
        "Diagramme de sequence": "Diagramme de séquence",
        "simplifie": "simplifié",
        "Ecran": "Écran",
        "Detail": "Détail",
        "modele": "modèle",
        "Modele": "Modèle",
        "reponse": "réponse",
        "deploiement": "déploiement",
        "amelioration": "amélioration",
        "corrigees": "corrigées",
        "configures": "configurés",
        "cles": "clés",
        "Cle": "Clé",
        "API valide": "API valide",
        "developpement mobile": "développement mobile",
        "stockage": "stockage",
        "Annexes": "Annexes",
        "acces": "accès",
        "selon": "selon",
    }

    def fix_text(text):
        for src, dst in replacements.items():
            text = text.replace(src, dst)
        return text

    for paragraph in doc.paragraphs:
        for run in paragraph.runs:
            if run.text:
                run.text = fix_text(run.text)
    for table in doc.tables:
        for row in table.rows:
            for cell in row.cells:
                for paragraph in cell.paragraphs:
                    for run in paragraph.runs:
                        if run.text:
                            run.text = fix_text(run.text)


def main():
    figs = {
        "architecture": OUT_DIR / "figure_architecture.png",
        "use_case": OUT_DIR / "figure_use_case.png",
        "activity": OUT_DIR / "figure_activity.png",
        "class": OUT_DIR / "figure_class.png",
        "chatbot": OUT_DIR / "figure_chatbot.png",
        "sequence_search": OUT_DIR / "figure_sequence_search.png",
        "sequence_chatbot": OUT_DIR / "figure_sequence_chatbot.png",
        "sequence_profile": OUT_DIR / "figure_sequence_profile.png",
    }
    save_architecture_diagram(figs["architecture"])
    save_use_case_diagram(figs["use_case"])
    save_activity_diagram(figs["activity"])
    save_class_diagram(figs["class"])
    save_chatbot_diagram(figs["chatbot"])
    save_sequence_diagram(
        figs["sequence_search"],
        "Diagramme de sequence : recherche de film",
        ["Utilisateur", "MainActivity", "TMDB API", "RecyclerView"],
        [
            (0, 1, "saisit un titre / utilise le micro"),
            (1, 2, "requete HTTP de recherche"),
            (2, 1, "retour JSON des films"),
            (1, 3, "mise a jour de la liste"),
            (0, 1, "selectionne un film"),
        ],
    )
    save_sequence_diagram(
        figs["sequence_chatbot"],
        "Diagramme de sequence : chatbot CineBot",
        ["Utilisateur", "ChatBotActivity", "Serveur Express", "Modele CineBot"],
        [
            (0, 1, "envoie un message"),
            (1, 2, "POST /api/chat"),
            (2, 3, "analyse de la demande"),
            (3, 2, "recommandation + film"),
            (2, 1, "JSON text, film, timestamp"),
            (1, 0, "affiche la reponse"),
        ],
    )
    save_sequence_diagram(
        figs["sequence_profile"],
        "Diagramme de sequence : photo de profil",
        ["Utilisateur", "ProfileActivity", "Camera", "Supabase", "Firestore"],
        [
            (0, 1, "appuie sur Prendre une photo"),
            (1, 2, "lance la camera"),
            (2, 1, "retour image bitmap"),
            (1, 3, "upload image JPEG"),
            (3, 1, "URL publique"),
            (1, 4, "sauvegarde metadata"),
        ],
    )

    doc = Document()
    configure_doc(doc)
    add_cover(doc)
    add_preliminaries(doc)
    add_intro(doc)
    add_chapter1(doc)
    add_chapter2(doc, figs)
    add_chapter3(doc, figs)
    add_chapter4(doc)
    add_extended_professional_sections(doc, figs)
    add_conclusion_refs_annexes(doc)
    apply_french_accents(doc)
    doc.save(DOCX_PATH_PRO)
    print(DOCX_PATH_PRO)


if __name__ == "__main__":
    main()
